package io.ak1.drawboxsample.save

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val context = LocalContext.current
    val saver = remember(context) { AndroidImageSaver(context.applicationContext) }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> saver.onJsonPicked(uri) }
    saver.jsonPicker = jsonLauncher
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> saver.onImagePicked(uri) }
    saver.imagePicker = imageLauncher
    return saver
}

private class AndroidImageSaver(private val context: Context) : ImageSaver {
    var jsonPicker: ActivityResultLauncher<Array<String>>? = null
    var imagePicker: ActivityResultLauncher<Array<String>>? = null
    private var pendingJsonCallback: ((String) -> Unit)? = null
    private var pendingImageCallback: ((ByteArray, Size) -> Unit)? = null

    override fun savePng(bytes: ByteArray) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val fileName = "DrawBox-${System.currentTimeMillis()}.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/DrawBox",
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                if (uri == null) {
                    showToast("Failed to create file in Pictures")
                    return@launch
                }

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }
                showToast("PNG saved successfully!")
            } catch (e: Throwable) {
                android.util.Log.e("ImageSaver", "Error saving PNG", e)
                showToast("Error saving PNG: ${e.message}")
            }
        }
    }

    override fun saveSvg(svgContent: String) {
        saveTextFile(svgContent, "svg", "image/svg+xml")
    }

    override fun saveJson(jsonContent: String) {
        saveTextFile(jsonContent, "json", "application/json")
    }

    override fun loadJson(onLoaded: (String) -> Unit) {
        val launcher = jsonPicker
        if (launcher == null) {
            showToast("Import not ready yet")
            return
        }
        pendingJsonCallback = onLoaded
        launcher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    override fun loadImage(onLoaded: (ByteArray, Size) -> Unit) {
        val launcher = imagePicker
        if (launcher == null) {
            showToast("Image picker not ready yet")
            return
        }
        pendingImageCallback = onLoaded
        launcher.launch(arrayOf("image/*"))
    }

    fun onImagePicked(uri: Uri?) {
        val callback = pendingImageCallback
        pendingImageCallback = null
        if (uri == null || callback == null) return
        GlobalScope.launch(Dispatchers.IO) {
            val payload = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Throwable) {
                android.util.Log.e("ImageSaver", "Error reading image", e)
                showToast("Error reading image: ${e.message}")
                null
            } ?: return@launch
            // Decode just bounds to recover intrinsic size — avoids loading the
            // full bitmap into memory twice.
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(payload, 0, payload.size, opts)
            val size = if (opts.outWidth > 0 && opts.outHeight > 0) {
                Size(opts.outWidth.toFloat(), opts.outHeight.toFloat())
            } else Size.Zero
            GlobalScope.launch(Dispatchers.Main) { callback(payload, size) }
        }
    }

    fun onJsonPicked(uri: Uri?) {
        val callback = pendingJsonCallback
        pendingJsonCallback = null
        if (uri == null || callback == null) return
        GlobalScope.launch(Dispatchers.IO) {
            val content = try {
                context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().decodeToString()
                }
            } catch (e: Throwable) {
                android.util.Log.e("ImageSaver", "Error reading JSON", e)
                showToast("Error reading JSON: ${e.message}")
                null
            }
            if (content != null) {
                GlobalScope.launch(Dispatchers.Main) { callback(content) }
            }
        }
    }

    private fun saveTextFile(content: String, extension: String, mimeType: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val fileName = "DrawBox-${System.currentTimeMillis()}.$extension"
                val values = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOCUMENTS + "/DrawBox",
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)

                if (uri == null) {
                    showToast("Failed to create $extension file")
                    return@launch
                }

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray())
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }
                showToast("${extension.uppercase()} saved successfully!")
            } catch (e: Throwable) {
                android.util.Log.e("ImageSaver", "Error saving $extension", e)
                showToast("Error saving $extension: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}