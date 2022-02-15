package io.ak1.drawbox

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Created by akshay on 18/01/22
 * https://ak1.io
 */
class DrawController internal constructor(val trackHistory: (undoCount: Int, redoCount: Int) -> Unit = { _, _ -> }) {

    private val _redoPathList = mutableStateListOf<PathWrapper>()
    private val _undoPathList = mutableStateListOf<PathWrapper>()
    val pathList: SnapshotStateList<PathWrapper> = _undoPathList


    var opacity by mutableStateOf(1f)
        private set

    var strokeWidth by mutableStateOf(10f)
        private set

    var color by mutableStateOf(Color.Red)
        private set

    fun changeOpacity(value: Float) { opacity = value }

    fun changeColor(value: Color) { color = value }

    fun changeStrokeWidth(value: Float) { strokeWidth = value }

    //internal var bitmap: Bitmap? = null


    fun importPath(path: ArrayList<PathWrapper>) {
        reset()
        _undoPathList.addAll(path)
    }


    fun exportPath() = pathList.toList()

    fun unDo() {
        if (_undoPathList.isNotEmpty()) {
            val last = _undoPathList.last()
            _redoPathList.add(last)
            _undoPathList.remove(last)
            trackHistory(_undoPathList.size, _redoPathList.size)
        }
    }

    fun reDo() {
        if (_undoPathList.isNotEmpty()) {
            val last = _redoPathList.last()
            _undoPathList.add(last)
            _redoPathList.remove(last)
            trackHistory(_undoPathList.size, _redoPathList.size)
        }
    }

    fun reset() {
        _redoPathList.clear()
        _undoPathList.clear()
    }

    fun updateLatestPath(newPoint: Offset) {
        val index = _undoPathList.lastIndex
        _undoPathList[index].points.add(newPoint)
    }

    fun insertNewPath(newPoint: Offset) {
        val pathWrapper = PathWrapper(
            points = mutableStateListOf(newPoint),
            strokeColor = color,
            alpha = opacity,
            strokeWidth = strokeWidth,
        )
        _undoPathList.add(pathWrapper)
    }


    fun getDrawBoxBitmap() = null //bitmap


}

@Composable
fun rememberDrawController(trackHistory: (undoCount: Int, redoCount: Int) -> Unit = { _, _ -> }): DrawController {
    return remember { DrawController(trackHistory) }
}