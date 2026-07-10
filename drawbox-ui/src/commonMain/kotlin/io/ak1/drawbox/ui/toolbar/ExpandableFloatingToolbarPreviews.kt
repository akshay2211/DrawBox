@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawbox.ui.toolbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/** Bar with three leaf items and no submenus. */
@Preview
@Composable
internal fun ExpandableFloatingToolbarBasicPreview() {
    PreviewSurface {
        ExpandableFloatingToolbar(
            items = listOf(
                FloatingMenuItem(
                    id = "add",
                    icon = { isActive ->
                        Icon(Icons.Filled.Add, "Add", tint = isActive.activeIconTint())
                    },
                    onClick = {},
                ),
                FloatingMenuItem(
                    id = "edit",
                    icon = { isActive ->
                        Icon(Icons.Filled.Edit, "Edit", tint = isActive.activeIconTint())
                    },
                    onClick = {},
                ),
                FloatingMenuItem(
                    id = "delete",
                    icon = { _ ->
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = {},
                ),
            ),
            modifier = Modifier.padding(24.dp),
        )
    }
}

/** Bar with one active parent slot; submenu rendered above. */
@Preview
@Composable
internal fun ExpandableFloatingToolbarSubmenuPreview() {
    PreviewSurface {
        ExpandableFloatingToolbar(
            items = listOf(
                FloatingMenuItem(
                    id = "edit",
                    isActive = true,
                    icon = { isActive ->
                        Icon(Icons.Filled.Edit, "Edit", tint = isActive.activeIconTint())
                    },
                    onClick = {},
                    children = listOf(
                        FloatingMenuItem(
                            id = "edit-add",
                            icon = { isActive ->
                                Icon(Icons.Filled.Add, "Add", tint = isActive.activeIconTint())
                            },
                        ),
                        FloatingMenuItem(
                            id = "edit-person",
                            isActive = true,
                            icon = { isActive ->
                                Icon(Icons.Filled.Person, "Person", tint = isActive.activeIconTint())
                            },
                        ),
                        FloatingMenuItem(
                            id = "edit-fav",
                            icon = { isActive ->
                                Icon(Icons.Filled.Favorite, "Favorite", tint = isActive.activeIconTint())
                            },
                        ),
                    ),
                ),
                FloatingMenuItem(
                    id = "more",
                    icon = { isActive ->
                        Icon(Icons.Filled.MoreVert, "More", tint = isActive.activeIconTint())
                    },
                    onClick = {},
                ),
            ),
            controlledActiveId = "edit",
            modifier = Modifier.padding(24.dp),
        )
    }
}

/** Submenu-below variant used by the top-anchored context bar. */
@Preview
@Composable
internal fun ExpandableFloatingToolbarBelowPreview() {
    PreviewSurface {
        ExpandableFloatingToolbar(
            items = listOf(
                FloatingMenuItem(
                    id = "size",
                    isActive = true,
                    icon = { isActive ->
                        Icon(Icons.Filled.Edit, "Size", tint = isActive.activeIconTint())
                    },
                    onClick = {},
                    children = listOf(
                        FloatingMenuItem(
                            id = "sm",
                            icon = { isActive ->
                                Icon(Icons.Filled.Add, "Small", tint = isActive.activeIconTint())
                            },
                        ),
                        FloatingMenuItem(
                            id = "md",
                            isActive = true,
                            icon = { isActive ->
                                Icon(Icons.Filled.Add, "Medium", tint = isActive.activeIconTint())
                            },
                        ),
                    ),
                ),
            ),
            controlledActiveId = "size",
            submenuPosition = SubmenuPosition.Below,
            modifier = Modifier.padding(24.dp),
        )
    }
}

/** Grouped bar using a [separator] between clusters (ControlsBar layout). */
@Preview
@Composable
internal fun ExpandableFloatingToolbarSeparatorPreview() {
    PreviewSurface {
        ExpandableFloatingToolbar(
            items = listOf(
                FloatingMenuItem(id = "a", icon = { isActive ->
                    Icon(Icons.Filled.Add, "A", tint = isActive.activeIconTint())
                }, onClick = {}),
                FloatingMenuItem(id = "b", icon = { isActive ->
                    Icon(Icons.Filled.Edit, "B", tint = isActive.activeIconTint())
                }, onClick = {}),
                separator("sep"),
                FloatingMenuItem(id = "c", isActive = true, icon = { isActive ->
                    Icon(Icons.Filled.Favorite, "C", tint = isActive.activeIconTint())
                }, onClick = {}),
                FloatingMenuItem(id = "d", icon = { isActive ->
                    Icon(Icons.Filled.Person, "D", tint = isActive.activeIconTint())
                }, onClick = {}),
            ),
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
internal fun PreviewSurface(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .size(width = 380.dp, height = 140.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
