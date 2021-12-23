package io.ak1.pencil

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Created by akshay on 10/12/21
 * https://ak1.io
 */
@Composable
fun Pencil() {
    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures({
            Log.e("detectDragGestures", "started")

        }, {
            Log.e("detectDragGestures", "ended")
        }, {
            Log.e("detectDragGestures", "canceled")
        }, { change, dragAmount ->

            Log.e(
                "change",
                "${change.position.x},${change.position.y} ${change.pressed} ${change.consumed.positionChange}  ${dragAmount.x},${dragAmount.y}"
            )

        })
        detectTapGestures(
            onPress = { /* Called when the gesture starts */ },
            onDoubleTap = { /* Called on Double Tap */ },
            onLongPress = { /* Called on Long Press */ },
            onTap = { /* Called on Tap */ }
        )
    }
    ) {

    }

}