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
    override fun savePng(bitmap: ImageBitmap) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save Drawing as PNG"
            fileFilter = FileNameExtensionFilter("PNG image", "png")
            selectedFile = File("DrawBox-${System.currentTimeMillis()}.png")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        val target = chooser.selectedFile.let {
            if (it.extension.equals("png", ignoreCase = true)) it else File("${it.absolutePath}.png")
        }
        val awt: BufferedImage = bitmap.toAwtImage()
        ImageIO.write(awt, "png", target)
        println("PNG saved to: ${target.absolutePath}")
    }

    override fun saveSvg(svgContent: String) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save Drawing as SVG"
            fileFilter = FileNameExtensionFilter("SVG image", "svg")
            selectedFile = File("DrawBox-${System.currentTimeMillis()}.svg")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        val target = chooser.selectedFile.let {
            if (it.extension.equals("svg", ignoreCase = true)) it else File("${it.absolutePath}.svg")
        }
        target.writeText(svgContent)
        println("SVG saved to: ${target.absolutePath}")
    }

    override fun saveJson(jsonContent: String) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Export Drawing as JSON"
            fileFilter = FileNameExtensionFilter("DrawBox JSON", "json")
            selectedFile = File("DrawBox-${System.currentTimeMillis()}.json")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        val target = chooser.selectedFile.let {
            if (it.extension.equals("json", ignoreCase = true)) it else File("${it.absolutePath}.json")
        }
        target.writeText(jsonContent)
        println("JSON saved to: ${target.absolutePath}")
    }

    override fun loadJson(onLoaded: (String) -> Unit) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Import Drawing from JSON"
            fileFilter = FileNameExtensionFilter("DrawBox JSON", "json")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return
        val source = chooser.selectedFile ?: return
        onLoaded(source.readText())
    }
}