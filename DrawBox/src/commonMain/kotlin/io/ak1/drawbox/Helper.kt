package io.ak1.drawbox

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.TextAlignment
import io.ak1.drawbox.text.FontRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun createPath(points: List<Offset>) = Path().apply {
    if (points.size > 1) {
        var oldPoint: Offset? = null
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val point = points[i]
            oldPoint?.let {
                val midPoint = calculateMidpoint(it, point)
                if (i == 1) {
                    lineTo(midPoint.x, midPoint.y)
                } else {
                    quadraticTo(it.x, it.y, midPoint.x, midPoint.y)
                }
            }
            oldPoint = point
        }
        oldPoint?.let { lineTo(it.x, it.y) }
    }
}

private fun calculateMidpoint(start: Offset, end: Offset) = Offset((start.x + end.x) / 2, (start.y + end.y) / 2)

/**
 * Allocation-free check: do the entries of [current] (in order, excluding those
 * whose id is in [activeIds] or [hiddenIds]) exactly match [reference] by
 * reference identity?
 *
 * Used as the freshness gate for the cached `finalizedLayer` recording in
 * `DrawBox`. The reducer always produces a fresh [Element] instance via
 * `copy(...)` on any mutation, so reference equality is both correct (catches
 * every meaningful change) and cheap (no field comparison).
 *
 * [hiddenIds] is what flips a cache that was recorded *before* the host
 * decided to hide an element. Without including it in the skip set, a Text
 * element about to be edited inline would replay from the cache and ghost
 * underneath the editor.
 *
 * Iterates `current` exactly once, no intermediate list, no boxed iterators.
 */
internal fun staticRefsMatch(
    current: List<Element>,
    activeIds: Set<String>,
    hiddenIds: Set<String>,
    reference: List<Element>,
): Boolean {
    var refIdx = 0
    for (idx in 0 until current.size) {
        val e = current[idx]
        if (e.id in activeIds || e.id in hiddenIds) continue
        if (refIdx >= reference.size) return false
        if (reference[refIdx] !== e) return false
        refIdx++
    }
    return refIdx == reference.size
}

/**
 * Returns the list sorted by [Element.zIndex], or the original instance when it
 * is already in ascending zIndex order. Insertion via `addElement` keeps the
 * list sorted by construction, so the common case is the O(N) scan with zero
 * allocations; only `bringToFront`/`sendToBack` or out-of-order undo state
 * triggers the actual sort.
 */
internal fun List<Element>.sortedByZIndexIfNeeded(): List<Element> {
    var prev = Int.MIN_VALUE
    for (e in this) {
        if (e.zIndex < prev) return this.sortedBy { it.zIndex }
        prev = e.zIndex
    }
    return this
}

/**
 * Per-canvas cache of [Path] objects keyed by [Element.id]. A cache hit requires
 * the stored points list to be the *same instance* as the lookup's points; any
 * mutation in the reducer produces a fresh list via `copy(points = ...)`, so
 * reference equality is both correct and free.
 *
 * Created with `remember { PathCache() }` so it survives recompositions. Stale
 * entries are reclaimed lazily — when the cache grows materially larger than
 * the current element set, [retainOnly] drops the orphans.
 */
internal class PathCache {
    private data class Entry(val pointsRef: List<Offset>, val path: Path)
    private val byId = HashMap<String, Entry>()

    fun pathFor(id: String, points: List<Offset>): Path {
        val existing = byId[id]
        if (existing != null && existing.pointsRef === points) return existing.path
        val fresh = createPath(points)
        byId[id] = Entry(points, fresh)
        return fresh
    }

    /**
     * Drop entries whose ids are no longer in [liveIds]. Cheap to call but not
     * free — only invoke when the live set actually shrank.
     */
    fun retainOnly(liveIds: Set<String>) {
        if (byId.isEmpty()) return
        byId.keys.retainAll(liveIds)
    }

    fun size(): Int = byId.size
}

/**
 * Per-canvas async cache of decoded [ImageBitmap] objects for
 * [io.ak1.drawbox.domain.model.Element.Image] elements. Decoding a typical
 * 1 MB PNG takes several milliseconds and used to run on the UI thread
 * inside the draw pass; this cache moves it onto [Dispatchers.Default] and
 * stores at mip-level (a power-of-two cap on the longer side) so a small
 * placement doesn't hold a source-resolution bitmap in memory.
 *
 * ### Lookup contract
 *
 * [bitmapFor] is **synchronous and non-blocking** — it returns whatever is
 * currently in the cache for `id`:
 *
 * - Exact hit (same `bytes` reference, same mip level): returns the cached
 *   bitmap.
 * - Mip-level mismatch (resize): returns the stale lower / higher-res
 *   bitmap immediately and kicks off a background decode at the new level.
 * - Bytes mismatch (image replaced): returns null (or stale) and kicks off
 *   a fresh decode.
 * - Cold miss: returns null and kicks off the first decode.
 *
 * The draw pass should treat null as "show placeholder"; the next
 * recomposition (triggered by the [androidx.compose.runtime.snapshots.SnapshotStateMap]
 * write) draws the bitmap.
 *
 * ### Race protection
 *
 * Each new decode request bumps a monotonic generation counter. When a
 * decode completes, it only updates the cache if the entry's generation
 * still matches — so a fast-resize storm that supersedes an in-flight
 * decode discards the stale result instead of clobbering the newer one.
 *
 * ### Mip levels
 *
 * The target dimension passed by the caller (typically `placedSize ×
 * viewport.scale`) is rounded **up** to the next power of two within
 * `[MIN_MIP_PX, MAX_MIP_PX]`. The decoded bitmap fits inside that cap on
 * its longer side; the shorter side scales proportionally.
 */
internal class ImageBitmapCache(
    private val scope: CoroutineScope,
) {
    private data class Entry(
        val gen: Long,
        val bytesRef: ByteArray,
        val mipLevelPx: Int,
        val bitmap: ImageBitmap?,
    )

    private val byId = mutableStateMapOf<String, Entry>()
    private var nextGen: Long = 0L

    /**
     * Returns the current cached bitmap for [id], starting a background
     * decode when the key differs from what's stored. The returned value
     * may be `null` (no bitmap available yet) or a stale-resolution bitmap
     * from a previous decode — both are valid "show this now" results.
     *
     * @param targetDim longer-side target in screen pixels (callers
     *   typically pass `max(placedWidth, placedHeight) × viewport.scale`).
     *   Used to pick a mip level; smaller targets get smaller bitmaps.
     */
    fun bitmapFor(id: String, bytes: ByteArray, targetDim: Int): ImageBitmap? {
        val mipLevelPx = mipLevelFor(targetDim)
        val existing = byId[id]
        if (existing != null &&
            existing.bytesRef === bytes &&
            existing.mipLevelPx == mipLevelPx
        ) {
            return existing.bitmap
        }

        // Key changed (bytes swapped, different mip level, or never decoded).
        // Bump the generation, write a provisional entry with the stale bitmap
        // (so the renderer can keep showing something), and kick off a fresh
        // decode in the background. Any prior in-flight decode for the same
        // id with an older generation will be discarded on completion.
        val myGen = ++nextGen
        val staleBitmap = existing?.bitmap
        byId[id] = Entry(
            gen = myGen,
            bytesRef = bytes,
            mipLevelPx = mipLevelPx,
            bitmap = staleBitmap,
        )
        scope.launch(Dispatchers.Default) {
            val decoded = decodeImageBitmapDownsampled(bytes, mipLevelPx, mipLevelPx)
            // Only land the result if our generation is still current. A
            // newer request that arrived during the decode (fast resize,
            // new bytes) supersedes us; we drop the result.
            val current = byId[id]
            if (current != null && current.gen == myGen) {
                byId[id] = current.copy(bitmap = decoded)
            }
        }
        return staleBitmap
    }

    fun retainOnly(liveIds: Set<String>) {
        if (byId.isEmpty()) return
        // SnapshotStateMap.keys.retainAll exists but does an iteration-time
        // check. Build the drop list first to avoid mutation while iterating.
        val drop = byId.keys.filter { it !in liveIds }
        for (id in drop) byId.remove(id)
    }

    fun size(): Int = byId.size

    private fun mipLevelFor(targetDim: Int): Int {
        val clamped = targetDim.coerceIn(MIN_MIP_PX, MAX_MIP_PX)
        var px = MIN_MIP_PX
        while (px < clamped) px *= 2
        return px
    }
}

/**
 * Smallest mip level the cache will produce. A 64×64 thumbnail still fits
 * in this cap, so we don't decode at sub-thumbnail resolutions.
 */
private const val MIN_MIP_PX: Int = 128

/**
 * Largest mip level. Caps memory at this resolution; placements drawn at
 * larger sizes will scale up via the GPU rather than the cache holding a
 * source-resolution bitmap. 2048 px is a reasonable ceiling for canvas
 * use cases (annotation, whiteboard) — much smaller than the photographic
 * source size for typical screenshots.
 */
private const val MAX_MIP_PX: Int = 2048

/**
 * Per-canvas cache of [TextLayoutResult] entries for [Element.Text]
 * elements. Laying out a multi-line block runs the platform text shaper —
 * cheap individually but cumulative across hundreds of elements per frame.
 *
 * The cache key is the tuple of fields that participate in layout: the
 * text content, font family key, font size, alignment, and wrap width.
 * Anything else (color, opacity, rotation, position) is applied at draw
 * time and doesn't invalidate the layout.
 *
 * Entries survive recompositions via `remember`; stale entries are
 * reclaimed by [retainOnly] when the live element set shrinks.
 */
internal class TextLayoutCache {
    private data class Key(
        val text: String,
        val fontFamilyKey: String,
        val fontSize: Float,
        val alignment: TextAlignment,
        val wrapWidth: Float,
    )
    private data class Entry(val key: Key, val layout: TextLayoutResult)
    private val byId = HashMap<String, Entry>()

    fun layoutFor(
        id: String,
        text: String,
        fontFamilyKey: String,
        fontSize: Float,
        alignment: TextAlignment,
        wrapWidth: Float,
        measurer: TextMeasurer,
    ): TextLayoutResult {
        val key = Key(text, fontFamilyKey, fontSize, alignment, wrapWidth)
        val existing = byId[id]
        if (existing != null && existing.key == key) return existing.layout
        val style = TextStyle(
            fontSize = fontSize.sp,
            fontFamily = FontRegistry.resolve(fontFamilyKey),
            textAlign = alignment.toComposeAlign(),
        )
        val layout = measurer.measure(
            text = text,
            style = style,
            constraints = Constraints(maxWidth = wrapWidth.toInt().coerceAtLeast(1)),
            softWrap = true,
        )
        byId[id] = Entry(key, layout)
        return layout
    }

    fun retainOnly(liveIds: Set<String>) {
        if (byId.isEmpty()) return
        byId.keys.retainAll(liveIds)
    }

    fun size(): Int = byId.size
}

private fun TextAlignment.toComposeAlign(): TextAlign = when (this) {
    TextAlignment.LEFT -> TextAlign.Left
    TextAlignment.CENTER -> TextAlign.Center
    TextAlignment.RIGHT -> TextAlign.Right
}
