package io.ak1.drawbox

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import io.ak1.drawbox.domain.model.Element


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
 * whose id is in [activeIds]) exactly match [reference] by reference identity?
 *
 * Used as the freshness gate for the cached `finalizedLayer` recording in
 * `DrawBox`. The reducer always produces a fresh [Element] instance via
 * `copy(...)` on any mutation, so reference equality is both correct (catches
 * every meaningful change) and cheap (no field comparison).
 *
 * Iterates `current` exactly once, no intermediate list, no boxed iterators.
 */
internal fun staticRefsMatch(
    current: List<Element>,
    activeIds: Set<String>,
    reference: List<Element>,
): Boolean {
    var refIdx = 0
    for (idx in 0 until current.size) {
        val e = current[idx]
        if (e.id in activeIds) continue
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
