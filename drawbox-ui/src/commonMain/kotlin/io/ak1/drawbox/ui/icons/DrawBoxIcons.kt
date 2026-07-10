package io.ak1.drawbox.ui.icons

import io.ak1.drawbox.ui.resources.Res
import io.ak1.drawbox.ui.resources.arrow
import io.ak1.drawbox.ui.resources.border_corner_pill
import io.ak1.drawbox.ui.resources.border_corner_rounded
import io.ak1.drawbox.ui.resources.border_corner_square
import io.ak1.drawbox.ui.resources.circle
import io.ak1.drawbox.ui.resources.dots_vertical
import io.ak1.drawbox.ui.resources.eraser
import io.ak1.drawbox.ui.resources.export
import io.ak1.drawbox.ui.resources.file_import
import io.ak1.drawbox.ui.resources.file_type_svg
import io.ak1.drawbox.ui.resources.hand_stop
import io.ak1.drawbox.ui.resources.letter_t
import io.ak1.drawbox.ui.resources.line
import io.ak1.drawbox.ui.resources.palette
import io.ak1.drawbox.ui.resources.png
import io.ak1.drawbox.ui.resources.pointer
import io.ak1.drawbox.ui.resources.redo
import io.ak1.drawbox.ui.resources.refresh
import io.ak1.drawbox.ui.resources.ruler
import io.ak1.drawbox.ui.resources.sketching
import io.ak1.drawbox.ui.resources.square
import io.ak1.drawbox.ui.resources.trash
import io.ak1.drawbox.ui.resources.triangle
import io.ak1.drawbox.ui.resources.undo
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
    val Eraser: DrawableResource = Res.drawable.eraser

    val BorderPill: DrawableResource = Res.drawable.border_corner_pill
    val BorderRounded: DrawableResource = Res.drawable.border_corner_rounded
    val BorderSquare: DrawableResource = Res.drawable.border_corner_square
    val Text: DrawableResource = Res.drawable.letter_t

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