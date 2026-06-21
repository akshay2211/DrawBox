@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ak1.drawboxsample.ui.theme.ThemeMode
import io.ak1.drawboxsample.ui.theme.next

/**
 * Top-right floating control cluster.
 *
 * Wide screens: [Theme] [Settings] only — zoom lives at bottom-left.
 * Narrow screens: [Zoom -/%/+] [Theme] [Settings] folded into a single bar so
 * there's only one floating control row on small viewports.
 */
@Composable
fun TopRightControls(
    isNarrow: Boolean,
    scalePercent: Int,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        contentPadding = PaddingValues(2.dp),
        modifier = modifier
            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
            .requiredHeight(40.dp),
        content = {
            if (isNarrow) {
                ZoomCluster(
                    scalePercent = scalePercent,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onZoomReset = onZoomReset,
                )
                VerticalDivider(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            ThemeToggleButton(themeMode = themeMode, onCycle = { onThemeModeChange(themeMode.next()) })
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
    )
}

/**
 * Standalone bottom-left zoom bar for wide screens.
 */
@Composable
fun ZoomToolbar(
    scalePercent: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        contentPadding = PaddingValues(2.dp),
        modifier = modifier
            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
            .requiredHeight(40.dp),
        content = {
            ZoomCluster(
                scalePercent = scalePercent,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onZoomReset = onZoomReset,
            )
        },
    )
}

@Composable
private fun RowScope.ZoomCluster(
    scalePercent: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
) {
    IconButton(
        onClick = onZoomOut,
        modifier = Modifier.size(36.dp),
    ) {
        Text("−", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
    }
    Box(
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .width(48.dp)
            .clickable(onClick = onZoomReset)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$scalePercent%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    IconButton(
        onClick = onZoomIn,
        modifier = Modifier.size(36.dp),
    ) {
        Text("+", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ThemeToggleButton(
    themeMode: ThemeMode,
    onCycle: () -> Unit,
) {
    val (vector, label) = when (themeMode) {
        ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto to "Theme: System"
        ThemeMode.LIGHT -> Icons.Filled.LightMode to "Theme: Light"
        ThemeMode.DARK -> Icons.Filled.DarkMode to "Theme: Dark"
    }
    IconButton(
        onClick = onCycle,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(imageVector = vector, contentDescription = label)
    }
}