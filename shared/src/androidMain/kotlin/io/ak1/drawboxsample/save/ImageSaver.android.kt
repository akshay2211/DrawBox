package io.ak1.drawboxsample.save

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val context = LocalContext.current
    return remember(context) { AndroidImageSaver(context.applicationContext) }
}

private class AndroidImageSaver(private val context: Context) : ImageSaver {
    override fun savePng(bitmap: ImageBitmap) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val androidBitmap = bitmap.asAndroidBitmap()
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
                    val success = androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    if (!success) {
                        throw Exception("Failed to compress bitmap")
                    }
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
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val fileName = "DrawBox-${System.currentTimeMillis()}.svg"
                val values = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "image/svg+xml")
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
                    showToast("Failed to create SVG file")
                    return@launch
                }

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(svgContent.toByteArray())
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }
                showToast("SVG saved successfully!")
            } catch (e: Throwable) {
                android.util.Log.e("ImageSaver", "Error saving SVG", e)
                showToast("Error saving SVG: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}