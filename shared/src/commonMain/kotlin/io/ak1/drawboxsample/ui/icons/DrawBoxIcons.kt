package io.ak1.drawboxsample.ui.icons

import drawboxsample.shared.generated.resources.Res
import drawboxsample.shared.generated.resources.arrow
import drawboxsample.shared.generated.resources.circle
import drawboxsample.shared.generated.resources.curved
import drawboxsample.shared.generated.resources.dots_vertical
import drawboxsample.shared.generated.resources.export
import drawboxsample.shared.generated.resources.file_import
import drawboxsample.shared.generated.resources.file_type_svg
import drawboxsample.shared.generated.resources.line
import drawboxsample.shared.generated.resources.palette
import drawboxsample.shared.generated.resources.png
import drawboxsample.shared.generated.resources.redo
import drawboxsample.shared.generated.resources.refresh
import drawboxsample.shared.generated.resources.ruler
import drawboxsample.shared.generated.resources.square
import drawboxsample.shared.generated.resources.triangle
import drawboxsample.shared.generated.resources.undo
import org.jetbrains.compose.resources.DrawableResource



object DrawBoxIcons {

    // Palette icon from SVG
    val Palette: DrawableResource = Res.drawable.palette

    // Rectangle icon from SVG
    val Rectangle: DrawableResource = Res.drawable.square

    // Circle icon from SVG
    val Circle: DrawableResource = Res.drawable.circle

    // Triangle icon from SVG
    val Triangle: DrawableResource = Res.drawable.triangle

    // Arrow icon from SVG
    val Arrow: DrawableResource = Res.drawable.arrow

    // Line icon (stroke straight from SVG)
    val Line: DrawableResource = Res.drawable.line

    // Refresh icon from SVG
    val RefreshIcon: DrawableResource = Res.drawable.refresh

    // Ruler/Size icon from SVG
    val Ruler: DrawableResource = Res.drawable.ruler

    // Pencil/Stroke curved icon from SVG
    val StrokeCurved: DrawableResource = Res.drawable.curved

    // File PNG icon from SVG
    val FilePng: DrawableResource = Res.drawable.png

    // File SVG icon from SVG
    val FileSvg: DrawableResource = Res.drawable.file_type_svg

    // Undo icon
    val Undo: DrawableResource = Res.drawable.undo

    // Redo icon
    val Redo: DrawableResource = Res.drawable.redo
    val Settings: DrawableResource = Res.drawable.dots_vertical
    val Import: DrawableResource = Res.drawable.file_import
    val Export: DrawableResource = Res.drawable.export

}
