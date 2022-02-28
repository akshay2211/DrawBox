package io.ak1.drawboxsample.ui.components

import android.widget.SeekBar
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import io.ak1.drawbox.DrawController
import io.ak1.drawboxsample.R


/**
 * Created by akshay on 29/12/21
 * https://ak1.io
 */


@Composable
fun ColorRow(
    isVisible: Boolean,
    rowElementsCount: Int = 8,
    colors: Array<Color>,
    clickedColor: (Color) -> Unit
) {
    val density = LocalDensity.current
    val defaultColor = remember {
        mutableStateOf(colors[0])
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically {
            // Slide in from 40 dp from the top.
            with(density) { -40.dp.roundToPx() }
        } + expandVertically(
            // Expand from the top.
            expandFrom = Alignment.Top
        ) + fadeIn(
            // Fade in with the initial alpha of 0.3f.
            initialAlpha = 0.3f
        ),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {

        var columnsSize: Int = colors.size / rowElementsCount
        val remaining = colors.size % rowElementsCount
        if (remaining > 0) {
            columnsSize++
        }
        Column(modifier = Modifier.padding(16.dp, 8.dp,16.dp, 16.dp)) {
            repeat(columnsSize) { column ->
                println()
                Row {
                    repeat(rowElementsCount) { row ->
                        val pos = (column * rowElementsCount) + row
                        var size = 22.dp
                        if (pos < colors.size) {
                            val color = colors[pos]
                            if (defaultColor.value == color) {
                                size = 36.dp
                            }

                            IconButton(
                                onClick = {
                                    defaultColor.value = color
                                    clickedColor(color)

                                }, modifier = Modifier
                                    .weight(1f, true)

                            ) {
                                Icon(
                                    painterResource(id = R.drawable.ic_color),
                                    contentDescription = stringResource(id = R.string.image_desc),
                                    tint = color,
                                    modifier = Modifier.size(size)
                                    //.animateContentSize(animationSpec = tween(1000,100,LinearOutSlowInEasing))
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f, true)
                            )
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun ControlsBar(
    drawController: DrawController,
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
                .clickable { if (undoVisibility.value) drawController.unDo() }
                .padding(12.dp)
                .weight(1f, true),
        )
        Image(
            painter = painterResource(id = R.drawable.ic_redo),
            contentDescription = "redo",
            colorFilter = ColorFilter.tint(if (redoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant),
            modifier = Modifier
                .clickable { if (redoVisibility.value) drawController.reDo() }
                .padding(12.dp)
                .weight(1f, true),
        )
        Image(
            painter = painterResource(id = R.drawable.ic_refresh),
            contentDescription = "reset",
            colorFilter = ColorFilter.tint(if (redoVisibility.value || undoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant),
            modifier = Modifier
                .clickable { drawController.reset() }
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
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically {
            // Slide in from 40 dp from the top.
            with(density) { -40.dp.roundToPx() }
        } + expandVertically(
            // Expand from the top.
            expandFrom = Alignment.Top
        ) + fadeIn(
            // Fade in with the initial alpha of 0.3f.
            initialAlpha = 0.3f
        ),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
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
                        onProgressChanged(p0?.progress ?: it.progress)
                    }
                })
            }
        }
    }
}