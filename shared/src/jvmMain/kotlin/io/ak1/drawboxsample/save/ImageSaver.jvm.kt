package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { DesktopImageSaver() }

private class DesktopImageSaver : ImageSaver {
    override fun save(bitmap: ImageBitmap) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save Drawing"
            fileFilter = FileNameExtensionFilter("PNG image", "png")
            selectedFile = File("DrawBox-${System.currentTimeMillis()}.png")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        val target = chooser.selectedFile.let {
            if (it.extension.equals("png", ignoreCase = true)) it else File("${it.absolutePath}.png")
        }
        val awt: BufferedImage = bitmap.toAwtImage()
        ImageIO.write(awt, "png", target)
    }
}