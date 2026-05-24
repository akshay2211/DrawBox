package io.ak1.drawboxsample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val palette = listOf(
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFF000000),
    Color(0xFFFFFFFF),
)

@Composable
fun ColorPalette(
    visible: Boolean,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
) {
    AnimatedVisibility(visible) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            palette.forEach { color ->
                val borderWidth = if (color == selectedColor) 2.dp else 0.5.dp
                val borderColor = if (color == selectedColor) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.surfaceVariant
                Row(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(color)
                        .border(borderWidth, borderColor, CircleShape).clickable { onColorSelected(color) },
                ) {}
            }
        }
    }
}
