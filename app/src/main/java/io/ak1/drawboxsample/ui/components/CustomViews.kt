package io.ak1.drawboxsample.ui.components

import android.widget.SeekBar
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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
    colors: List<Color>,
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
            with(density) {
                -40.dp.roundToPx()
            }
        } + expandVertically(
            // Expand from the top.
            expandFrom = Alignment.Top
        ) + fadeIn(
            // Fade in with the initial alpha of 0.3f.
            initialAlpha = 0.3f
        ),
        exit = slideOutVertically() + shrinkVertically(
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        val parentList = colors.chunked(rowElementsCount)

        Column(modifier = Modifier.padding(16.dp, 0.dp)) {
            parentList.forEachIndexed { _, colorRow ->
                Row {
                    repeat(rowElementsCount) { rowIndex ->
                        if (colorRow.size - 1 < rowIndex) {
                            Spacer(Modifier.weight(1f, true))
                            return@repeat
                        }
                        val color = colorRow[rowIndex]
                        ColorDots(color, color == defaultColor.value, 24.dp, 36.dp) {
                            defaultColor.value = it
                            clickedColor(color)
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun RowScope.ColorDots(
    color: Color,
    selected: Boolean,
    defaultSize: Dp,
    expandedSize: Dp,
    clickedColor: (Color) -> Unit
) {
    val dbAnimateAsState: Dp by animateDpAsState(
        targetValue = if (selected) expandedSize else defaultSize
    )
    IconButton(
        onClick = {
            clickedColor(color)
        }, modifier = Modifier
            .weight(1f, true)
    ) {
        Icon(
            painterResource(id = R.drawable.ic_color),
            contentDescription = stringResource(id = R.string.image_desc),
            tint = color,
            modifier = Modifier.size(dbAnimateAsState)
        )
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
        MenuItems(
            R.drawable.ic_download,
            "download",
            if (undoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant
        ) {
            if (undoVisibility.value) onDownloadClick()
        }
        MenuItems(
            R.drawable.ic_undo,
            "undo",
            if (undoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant
        ) {
            if (undoVisibility.value) drawController.unDo()
        }
        MenuItems(
            R.drawable.ic_redo,
            "redo",
            if (redoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant
        ) {
            if (redoVisibility.value) drawController.reDo()
        }
        MenuItems(
            R.drawable.ic_refresh,
            "reset",
            if (redoVisibility.value || undoVisibility.value) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant
        ) {
            drawController.reset()
        }
        MenuItems(R.drawable.ic_color, "stroke color", colorValue.value) {
            onColorClick()
        }
        MenuItems(R.drawable.ic_size, "stroke size", MaterialTheme.colors.primary) {
            onSizeClick()
        }
    }
}

@Composable
fun RowScope.MenuItems(
    @DrawableRes resId: Int,
    desc: String,
    colorTint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick, modifier = Modifier.weight(1f, true)
    ) {
        Icon(
            painterResource(id = resId),
            contentDescription = desc,
            tint = colorTint,
            modifier = Modifier.size(24.dp)
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