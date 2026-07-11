package io.ak1.drawboxsample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.DrawingPreview
import io.ak1.drawbox.domain.model.Element
import kotlinx.coroutines.delay

/**
 * Full-screen overlay that replays the drawing stroke-by-stroke in `createdAt`
 * order. Slider scrubs progress; play/pause animates. Read-only — no gestures
 * reach the canvas.
 *
 * Elements without a timestamp (createdAt == 0L) sort before timestamped ones,
 * which is the right behavior for back-compat with drawings created pre-2026.
 */
@Composable
fun ReplayScreen(
    elements: List<Element>,
    bgColor: Color,
    onClose: () -> Unit,
) {
    val sorted = remember(elements) {
        elements.withIndex()
            .sortedWith(compareBy({ it.value.createdAt }, { it.index }))
            .map { it.value }
    }
    val total = sorted.size

    var visibleCount by remember(total) { mutableIntStateOf(total) }
    var isPlaying by remember { mutableStateOf(false) }

    // Drive playback. ~250ms per element gives a watchable cadence for sketches;
    // for very long drawings the user can scrub instead.
    LaunchedEffect(isPlaying, total) {
        if (!isPlaying || total == 0) return@LaunchedEffect
        if (visibleCount >= total) visibleCount = 0
        while (isPlaying && visibleCount < total) {
            delay(250L)
            visibleCount += 1
        }
        if (visibleCount >= total) isPlaying = false
    }

    val visibleElements = remember(sorted, visibleCount) {
        sorted.subList(0, visibleCount.coerceIn(0, total))
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        DrawingPreview(
            elements = visibleElements,
            bgColor = bgColor,
            modifier = Modifier.fillMaxSize(),
        )

        // Close in top-right
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close replay")
        }

        // Scrubber pill bottom-center
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .fillMaxWidth(0.85f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (total == 0) return@IconButton
                        if (visibleCount >= total) visibleCount = 0
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    val playable = total > 0
                    Icon(
                        imageVector = when {
                            !playable -> Icons.Filled.PlayArrow
                            isPlaying -> Icons.Filled.Pause
                            visibleCount >= total -> Icons.Filled.Replay
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (playable) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Slider(
                    value = visibleCount.toFloat(),
                    onValueChange = {
                        isPlaying = false
                        visibleCount = it.toInt()
                    },
                    valueRange = 0f..total.toFloat().coerceAtLeast(0f),
                    steps = (total - 1).coerceAtLeast(0),
                    enabled = total > 0,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(
                    text = "$visibleCount / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}
