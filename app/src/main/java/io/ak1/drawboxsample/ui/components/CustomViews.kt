package io.ak1.drawboxsample.ui.components

import android.util.Log
import android.widget.SeekBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import io.ak1.drawboxsample.R
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
                .height(100.dp)
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
                            .size(25.dp)
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
    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
        Image(
            painter = painterResource(id = R.drawable.ic_download),
            contentDescription = "download",
            colorFilter = ColorFilter.tint(if (undoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant),
            modifier = Modifier
                .clickable { if (undoVisibility.value) onDownloadClick() }
                .padding(12.dp)
                .weight(1f, true),
        )
        Image(
            painter = painterResource(id = R.drawable.ic_undo),
            contentDescription = "undo",
            colorFilter = ColorFilter.tint(if (undoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant),
            modifier = Modifier
                .clickable { if (undoVisibility.value) unDo() }
                .padding(12.dp)
                .weight(1f, true),
        )
        Image(
            painter = painterResource(id = R.drawable.ic_redo),
            contentDescription = "redo",
            colorFilter = ColorFilter.tint(if (redoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant),
            modifier = Modifier
                .clickable { if (redoVisibility.value) reDo() }
                .padding(12.dp)
                .weight(1f, true),
        )
        Image(
            painter = painterResource(id = R.drawable.ic_refresh),
            contentDescription = "reset",
            colorFilter = ColorFilter.tint(if (redoVisibility.value || undoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant),
            modifier = Modifier
                .clickable { reset() }
                .padding(12.dp)
                .weight(1f, true),
        )
        Image(
            painter = painterResource(id = R.drawable.ic_color),
            contentDescription = "stroke color",
            colorFilter = ColorFilter.tint(colorValue.value),
            modifier = Modifier
                .clickable { onColorClick() }
                .padding(12.dp)
                .weight(1f, true),
        )
        Image(
            painter = painterResource(id = R.drawable.ic_size),
            contentDescription = "stroke size",
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
            modifier = Modifier
                .clickable { onSizeClick() }
                .padding(12.dp)
                .weight(1f, true),
        )

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
                .height(100.dp)
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
                it.thumb.colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
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