package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

interface ImageSaver {
    fun save(bitmap: ImageBitmap)
}

@Composable
expect fun rememberImageSaver(): ImageSaver