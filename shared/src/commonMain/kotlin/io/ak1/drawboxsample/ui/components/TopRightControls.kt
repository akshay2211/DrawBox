@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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

/**
 * Top-right floating control cluster.
 *
 * Wide screens: just [Settings] — zoom lives at bottom-left, theme moved into
 * the settings drawer.
 * Narrow screens: [Zoom -/%/+] | [Settings] folded into a single bar so there's
 * only one floating control row on small viewports.
 */
@Composable
fun TopRightControls(
    isNarrow: Boolean,
    scalePercent: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
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
    expanded: Boolean = false,
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
        Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
    }
    // Collapse the percent readout to zero width at exact 100% — the toolbar
    // contracts horizontally so [-] and [+] sit right next to each other, and
    // expands back with a spring-like tween on the first non-100 tick. Reset
    // gesture only exists when the label is visible (nothing to reset TO at
    // 100%).
    val labelWidth by animateDpAsState(
        targetValue = if (scalePercent == 100) 0.dp else 48.dp,
        animationSpec = tween(durationMillis = 180),
        label = "zoomLabelWidth",
    )
    Box(
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .width(labelWidth)
            .then(
                if (scalePercent != 100) Modifier.clickable(onClick = onZoomReset)
                else Modifier,
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (scalePercent != 100) {
            Text(
                text = "$scalePercent%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    IconButton(
        onClick = onZoomIn,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Zoom in")
    }
}

