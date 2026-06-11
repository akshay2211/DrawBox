package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ak1.drawbox.domain.model.Mode

@Composable
fun ShapeSelector(
    selectedShape: Mode,
    onShapeSelected: (Mode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(8.dp)
    ) {
        Text(
            "Drawing Mode: $selectedShape",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShapeModeButton(
                label = "↖",
                isSelected = selectedShape == Mode.SELECT,
                onClick = { onShapeSelected(Mode.SELECT) }
            )
            ShapeModeButton(
                label = "✏️",
                isSelected = selectedShape == Mode.PEN,
                onClick = { onShapeSelected(Mode.PEN) }
            )
            ShapeModeButton(
                label = "◻️",
                isSelected = selectedShape == Mode.RECTANGLE,
                onClick = { onShapeSelected(Mode.RECTANGLE) }
            )
            ShapeModeButton(
                label = "⭕",
                isSelected = selectedShape == Mode.CIRCLE,
                onClick = { onShapeSelected(Mode.CIRCLE) }
            )
            ShapeModeButton(
                label = "△",
                isSelected = selectedShape == Mode.TRIANGLE,
                onClick = { onShapeSelected(Mode.TRIANGLE) }
            )
            ShapeModeButton(
                label = "→",
                isSelected = selectedShape == Mode.ARROW,
                onClick = { onShapeSelected(Mode.ARROW) }
            )
            ShapeModeButton(
                label = "─",
                isSelected = selectedShape == Mode.LINE,
                onClick = { onShapeSelected(Mode.LINE) }
            )
        }
    }
}

@Composable
private fun RowScope.ShapeModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .weight(1f)
            .height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 16.sp)
    }
}
