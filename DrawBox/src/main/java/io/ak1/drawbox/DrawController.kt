package io.ak1.drawbox

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by akshay on 18/01/22
 * https://ak1.io
 */
class DrawController internal constructor() {
    internal var strokeWidth = 5f
    internal var strokeColor = Color.Red

    // TODO: 18/01/22 convert datatype to Stack
    //  currently Stack is internal in 'androidx.compose.runtime'
    internal val undoStack = ArrayList<PathWrapper>()
    internal val redoStack = ArrayList<PathWrapper>()
    internal var refreshState = UUID.randomUUID().toString()

    private var bitmap: Bitmap? = null


    fun importPath(path: ArrayList<PathWrapper>) = undoStack.apply {
        this.clear()
        this.addAll(path)
    }

    fun exportPath() = undoStack

    fun executeRefresh() {
        refreshState = UUID.randomUUID().toString()
    }

    fun setStrokeColor(color: Color) {
        strokeColor = color
    }

    fun setStrokeWidth(width: Float) {
        strokeWidth = width
    }

    fun unDo() {
        if (undoStack.isNotEmpty()) {
            val last = undoStack.last()
            redoStack.add(last)
            undoStack.remove(last)
            bitmap?.eraseColor(android.graphics.Color.TRANSPARENT)
        }
        emit("${undoStack.size}")
    }

    fun reDo() {
        if (redoStack.isNotEmpty()) {
            val last = redoStack.last()
            undoStack.add(last)
            redoStack.remove(last)
            bitmap?.eraseColor(android.graphics.Color.TRANSPARENT)
        }
        emit("${undoStack.size}")
    }

    fun reset() {
        redoStack.clear()
        undoStack.clear()
        bitmap?.eraseColor(android.graphics.Color.TRANSPARENT)
        emit(UUID.randomUUID().toString())
    }


    fun getDrawBoxBitmap() = bitmap


    internal fun generateCanvas(size: IntSize): Canvas? = if (size.width > 0 && size.height > 0) {
        bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap!!.asImageBitmap())
    } else null


    private val _changeRequests = MutableSharedFlow<String>()
    internal val changeRequests = _changeRequests.asSharedFlow()

    private fun emit(state: String = "") {
        _changeRequests.tryEmit(state)
    }

}

@Composable
fun rememberDrawController(): DrawController {
    return remember { DrawController() }
}