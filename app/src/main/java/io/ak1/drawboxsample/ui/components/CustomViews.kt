package io.ak1.drawboxsample.ui.components

import android.util.Log
import android.widget.SeekBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import io.ak1.drawbox.reDo
import io.ak1.drawbox.reset
import io.ak1.drawbox.unDo
import io.ak1.drawboxsample.data.local.arrayOfColors

/**
 * Created by akshay on 29/12/21
 * https://ak1.io
 */
@Composable
fun ColorRow(isVisible: Boolean, clicked: (Color) -> Unit) {
    if (isVisible) {
        Column(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = "Colors", modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(arrayOfColors) { item ->
                    Image(
                        painter = ColorPainter(item),
                        contentDescription = "hi",
                        Modifier
                            .padding(10.dp)
                            .size(35.dp)
                            .clip(
                                CircleShape
                            )
                            .clickable {
                                clicked.invoke(item)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun ControlsBar(
    onDownloadClick: () -> Unit,
    onColorClick: () -> Unit,
    onSizeClick: () -> Unit,
    undoVisibility: MutableState<Boolean>,
    redoVisibility: MutableState<Boolean>,
    colorValue: MutableState<Color>,
    sizeValue: MutableState<Int>
) {
    Row(modifier = Modifier.padding(12.dp)) {
        /*Button(onClick = onDownloadClick) {
            Text(text = "download")
        }*/
        Button(onClick = { unDo() }, enabled = undoVisibility.value) {
            Text(text = "unDo")
        }
        Button(onClick = { reDo() }, enabled = redoVisibility.value) {
            Text(text = "reDo")
        }
        Button(
            onClick = { reset() },
            enabled = redoVisibility.value || undoVisibility.value
        ) {
            Text(text = "reset")
        }

        Image(
            painter = ColorPainter(colorValue.value),
            contentDescription = "hi",
            Modifier
                .padding(12.dp,2.dp,12.dp,2.dp)
                .size(32.dp)
                .clip(
                    CircleShape
                )
                .clickable {
                    onColorClick()
                }
        )

        Button(onClick = onSizeClick) {
            Text(text = "${sizeValue.value} size")
        }
    }
}

@Composable
fun CustomSeekbar(
    isVisible: Boolean,
    max: Int = 200,
    progress: Int = max,
    progressColor: Int,
    thumbColor: Int,
    onProgressChanged: (Int) -> Unit
) {
    if (isVisible) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = "Stroke Width", modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp))
            AndroidView(
                { SeekBar(context) },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                it.progressDrawable.colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        progressColor,
                        BlendModeCompat.SRC_ATOP
                    )
                it.thumb.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    thumbColor,
                    BlendModeCompat.SRC_ATOP
                )
                it.max = max
                it.progress = progress
                it.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        Log.e("Progress", "-> ${p0?.progress}")
                        onProgressChanged(p0?.progress ?: it.progress)
                    }
                })
            }
        }
    }
}