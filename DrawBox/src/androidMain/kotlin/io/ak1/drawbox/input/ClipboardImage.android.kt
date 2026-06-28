package io.ak1.drawbox.input

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.geometry.Size
import java.io.ByteArrayOutputStream

/**
 * Android clipboard paste. Reads `ClipboardManager.primaryClip` for an
 * image URI (covers Files / Gallery / Photos "Copy" flows plus browser
 * "Copy image" on most Chromium derivatives), resolves it through the
 * application `ContentResolver`, and delivers the bytes + intrinsic
 * pixel size on the main thread.
 *
 * Targets tablets / Chromebooks / DeX where Cmd+V is a real flow, and
 * also covers a host-supplied "paste" toolbar button on phones. Plain
 * phones with software keyboards never hit Cmd+V, but a paste button
 * binds to the same function so the implementation is reused.
 *
 * ## Why reflection for context
 * The expect signature is a plain top-level function, not `@Composable`
 * — it's called from `onPreviewKeyEvent`, which itself isn't
 * `@Composable`. Reaching `LocalContext.current` from there is not
 * possible without API-level changes that cascade through every actual
 * + the host call site. `ActivityThread.currentApplication()` is a
 * private-but-stable Android internal that returns the live
 * `Application` instance; this is the canonical pattern used by
 * LeakCanary, ProcessLifecycleOwner, Coil, and others for the same
 * reason. If R8 strips `ActivityThread`, the call returns null and
 * paste no-ops gracefully.
 */
actual fun pasteImageFromClipboard(
    onLoaded: (bytes: ByteArray, intrinsicSize: Size) -> Unit,
) {
    val app = applicationContext() ?: run {
        Log.w("DrawBox", "Paste: no Application context reachable; skipping.")
        return
    }
    val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    val clip: ClipData = cm.primaryClip ?: return
    if (clip.itemCount == 0) return

    val resolver = app.contentResolver
    // Decoding can hit disk through ContentResolver — run off the main
    // thread. The host's onPreviewKeyEvent fires on the UI thread; we
    // marshal the result back via Handler so SDK state writes happen
    // where Compose expects them.
    Thread({
        val payload = (0 until clip.itemCount).firstNotNullOfOrNull { i ->
            val item = clip.getItemAt(i)
            val uri = item.uri ?: return@firstNotNullOfOrNull null
            uri.toImagePayload(resolver)
        } ?: run {
            Log.i("DrawBox", "Paste: clipboard had no resolvable image URI.")
            return@Thread
        }
        mainHandler.post { onLoaded(payload.bytes, payload.size) }
    }, "drawbox-clipboard-paste").apply { isDaemon = true }.start()
}

private data class ImagePayload(val bytes: ByteArray, val size: Size)

private fun Uri.toImagePayload(resolver: ContentResolver): ImagePayload? {
    val raw = try {
        resolver.openInputStream(this)?.use { it.readBytes() }
    } catch (e: Throwable) {
        null
    } ?: return null

    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(raw, 0, raw.size, opts)
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

    // If the source is already PNG, ship the bytes as-is; otherwise
    // re-encode through the bitmap path so the SDK's decoder sees a
    // known PNG container (matches the JVM + iOS behaviour).
    val mime = opts.outMimeType
    val bytes = if (mime == "image/png") {
        raw
    } else {
        val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
        ByteArrayOutputStream(64 * 1024).use { out ->
            if (!bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)) {
                return null
            }
            out.toByteArray()
        }
    }
    return ImagePayload(bytes, Size(opts.outWidth.toFloat(), opts.outHeight.toFloat()))
}

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

/**
 * Reach the process `Application` via `ActivityThread.currentApplication`
 * without requiring an explicit Context parameter. See class kdoc for
 * the rationale.
 */
private fun applicationContext(): Application? = runCatching {
    Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as? Application
}.getOrNull()
