package io.ak1.drawboxsample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.DrawController
import io.ak1.drawboxsample.ui.icons.DrawBoxIcons

@Composable
fun ControlsBar(
    drawController: DrawController,
    onDownloadClick: () -> Unit,
    onColorClick: () -> Unit,
    onBgColorClick: () -> Unit,
    onSizeClick: () -> Unit,
    undoVisibility: MutableState<Boolean>,
    redoVisibility: MutableState<Boolean>,
    colorValue: MutableState<Color>,
    bgColorValue: MutableState<Color>,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuItem(DrawBoxIcons.Download, "download", if (undoVisibility.value) active else inactive) {
            if (undoVisibility.value) onDownloadClick()
        }
        MenuItem(DrawBoxIcons.Undo, "undo", if (undoVisibility.value) active else inactive) {
            if (undoVisibility.value) drawController.unDo()
        }
        MenuItem(DrawBoxIcons.Redo, "redo", if (redoVisibility.value) active else inactive) {
            if (redoVisibility.value) drawController.reDo()
        }
        MenuItem(
            DrawBoxIcons.Refresh,
            "reset",
            if (redoVisibility.value || undoVisibility.value) active else inactive,
        ) {
            drawController.reset()
        }
        MenuItem(
            DrawBoxIcons.Color,
            "background color",
            bgColorValue.value,
            border = bgColorValue.value == MaterialTheme.colorScheme.background,
        ) { onBgColorClick() }
        MenuItem(DrawBoxIcons.Color, "stroke color", colorValue.value) { onColorClick() }
        MenuItem(DrawBoxIcons.Size, "stroke size", active) { onSizeClick() }
    }
}

@Composable
private fun RowScope.MenuItem(
    icon: ImageVector,
    desc: String,
    tint: Color,
    border: Boolean = false,
    onClick: () -> Unit,
) {
    val iconModifier = Modifier.size(20.dp)
    IconButton(onClick = onClick, modifier = Modifier.weight(1f).size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = tint,
            modifier = if (border)
                iconModifier.border(0.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            else
                iconModifier,
        )
    }
}

@Composable
fun StrokeSizeSlider(
    isVisible: Boolean,
    progress: Int,
    max: Int = 200,
    color: Color,
    onProgressChanged: (Int) -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -40 } + expandVertically(expandFrom = Alignment.Top) + fadeIn(initialAlpha = 0.3f),
        exit = slideOutVertically() + shrinkVertically() + fadeOut(),
    ) {
        Column(
            modifier = Modifier.height(100.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = "Stroke Width",
                modifier = Modifier.padding(start = 12.dp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Slider(
                value = progress.toFloat(),
                onValueChange = { onProgressChanged(it.toInt()) },
                valueRange = 1f..max.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
