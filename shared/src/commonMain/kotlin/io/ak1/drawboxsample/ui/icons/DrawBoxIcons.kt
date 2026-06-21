package io.ak1.drawboxsample.ui.icons

import drawboxsample.shared.generated.resources.Res
import drawboxsample.shared.generated.resources.arrow
import drawboxsample.shared.generated.resources.border_corner_pill
import drawboxsample.shared.generated.resources.border_corner_rounded
import drawboxsample.shared.generated.resources.border_corner_square
import drawboxsample.shared.generated.resources.circle
import drawboxsample.shared.generated.resources.curved
import drawboxsample.shared.generated.resources.dots_vertical
import drawboxsample.shared.generated.resources.export
import drawboxsample.shared.generated.resources.file_import
import drawboxsample.shared.generated.resources.file_type_svg
import drawboxsample.shared.generated.resources.hand_stop
import drawboxsample.shared.generated.resources.line
import drawboxsample.shared.generated.resources.palette
import drawboxsample.shared.generated.resources.png
import drawboxsample.shared.generated.resources.pointer
import drawboxsample.shared.generated.resources.redo
import drawboxsample.shared.generated.resources.refresh
import drawboxsample.shared.generated.resources.ruler
import drawboxsample.shared.generated.resources.sketching
import drawboxsample.shared.generated.resources.square
import drawboxsample.shared.generated.resources.trash
import drawboxsample.shared.generated.resources.triangle
import drawboxsample.shared.generated.resources.undo
import org.jetbrains.compose.resources.DrawableResource

object DrawBoxIcons {
    // Shapes
    val Palette: DrawableResource = Res.drawable.palette
    val Rectangle: DrawableResource = Res.drawable.square
    val Circle: DrawableResource = Res.drawable.circle
    val Triangle: DrawableResource = Res.drawable.triangle
    val Arrow: DrawableResource = Res.drawable.arrow
    val Line: DrawableResource = Res.drawable.line
    val Sketch: DrawableResource = Res.drawable.sketching
    val Trash: DrawableResource = Res.drawable.trash
    val Hand: DrawableResource = Res.drawable.hand_stop

    val BorderPill: DrawableResource = Res.drawable.border_corner_pill
    val BorderRounded: DrawableResource = Res.drawable.border_corner_rounded
    val BorderSquare: DrawableResource = Res.drawable.border_corner_square

    // Drawing Tools
    val Ruler: DrawableResource = Res.drawable.ruler
    val StrokeCurved: DrawableResource = Res.drawable.sketching

    // Actions
    val Refresh: DrawableResource = Res.drawable.refresh
    val Undo: DrawableResource = Res.drawable.undo
    val Redo: DrawableResource = Res.drawable.redo
    val Pointer: DrawableResource = Res.drawable.pointer

    // File Formats
    val FilePng: DrawableResource = Res.drawable.png
    val FileSvg: DrawableResource = Res.drawable.file_type_svg

    // Menu & UI
    val Settings: DrawableResource = Res.drawable.dots_vertical
    val Import: DrawableResource = Res.drawable.file_import
    val Export: DrawableResource = Res.drawable.export
}
