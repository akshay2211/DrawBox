package io.ak1.drawbox.ui.context

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment
import io.ak1.drawbox.ui.icons.DrawBoxIcons
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ColorSwatchIcon(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

/**
 * Same shape as [ColorSwatchIcon] but adds a slash overlay when the stroke is
 * disabled — visually pairs with [FillSwatchIcon]'s null-state.
 */
@Composable
internal fun StrokeSwatchIcon(color: Color, enabled: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (enabled) color else Color.Transparent)
            .border(1.dp, outline, CircleShape),
    ) {
        if (!enabled) DiagonalSlash(outline)
    }
}

@Composable
internal fun StrokeNoneIcon(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(1.dp, color, CircleShape),
    ) {
        DiagonalSlash(color)
    }
}

@Composable
internal fun FillSwatchIcon(color: Color?) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(shape)
            .background(color ?: Color.Transparent)
            .border(1.dp, outline, shape),
    ) {
        if (color == null) DiagonalSlash(outline)
    }
}

@Composable
internal fun FillNoneIcon(color: Color) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(shape)
            .border(1.dp, color, shape),
    ) {
        DiagonalSlash(color)
    }
}

@Composable
private fun DiagonalSlash(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun StrokeStyleIcon(style: StrokeStyle, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val y = size.height / 2f
        val effect = when (style) {
            StrokeStyle.SOLID -> null
            StrokeStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            StrokeStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5f, 4f))
        }
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
            pathEffect = effect,
        )
    }
}

@Composable
internal fun SizeDot(width: Float, selected: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = if (selected) {
                Modifier.size(width.dp).background(color, CircleShape)
            } else {
                Modifier.size(width.dp).border(1.dp, color, CircleShape)
            },
        )
    }
}

/**
 * Glyph-on-a-chip for a font-size preset. The glyph itself scales with [size]
 * (clamped to a chip-friendly range) so the chip visually reads as "small" /
 * "medium" / "huge" without the user having to read a number.
 */
@Composable
internal fun TextSizeIcon(size: Float, color: Color) {
    val display = size.coerceIn(8f, 28f)
    BasicText(
        text = "A",
        style = TextStyle(
            color = color,
            fontSize = TextUnit(display, TextUnitType.Sp),
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

@Composable
internal fun TextAlignmentIcon(alignment: TextAlignment, color: Color) {
    val icon = when (alignment) {
        TextAlignment.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
        TextAlignment.CENTER -> Icons.Filled.FormatAlignCenter
        TextAlignment.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
    }
    Icon(icon, contentDescription = alignment.name, tint = color)
}

/**
 * Three-letter glyph for the family key — written in the family itself so the
 * chip previews the typeface. Falls back to the key's first two characters if
 * no font family is resolved for the key.
 */
@Composable
internal fun FontFamilyLabel(
    key: String,
    color: Color,
    fontFamily: FontFamily?,
) {
    val label = when (key) {
        "sans" -> "Sa"
        "serif" -> "Se"
        "mono" -> "Mo"
        else -> key.take(2).replaceFirstChar { it.uppercase() }
    }
    BasicText(
        text = label,
        style = TextStyle(
            color = color,
            fontFamily = fontFamily,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

@Composable
internal fun CornerRadiusIcon(radius: Float, color: Color) {
    val cornerIcon = when {
        radius < 1f -> DrawBoxIcons.BorderSquare
        radius < 24f -> DrawBoxIcons.BorderRounded
        else -> DrawBoxIcons.BorderPill
    }
    Icon(
        painterResource(cornerIcon),
        contentDescription = "Corner radius",
        tint = color,
    )
}
