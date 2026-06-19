@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HomeMini
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One slot in [ExpandableFloatingToolbar]. Top-level items become the buttons
 * in the horizontal bar; nested [children] (if any) become the vertical sub-bar
 * shown above (or below — see [SubmenuPosition]) the item when it is active.
 */
data class FloatingMenuItem(
    val id: String,
    val icon: @Composable () -> Unit,
    val children: List<FloatingMenuItem> = emptyList(),
    val onClick: (() -> Unit)? = null,
)

/**
 * Direction the vertical submenu pops in. [Above] suits bottom-anchored bars
 * (default), [Below] suits top-anchored bars like the contextual config bar.
 */
enum class SubmenuPosition { Above, Below }

/**
 * A two-axis floating toolbar.
 *
 * Renders a [HorizontalFloatingToolbar] of [items]. Tapping an item with
 * [FloatingMenuItem.children] opens a [VerticalFloatingToolbar] above it,
 * horizontally aligned to the item's center. Tapping the same item again, or
 * tapping outside, dismisses it.
 *
 * @param controlledActiveId pass a non-null value to drive selection from the
 *   caller (and listen via [onActiveIdChange]); leave null for internal state.
 */
@Composable
fun ExpandableFloatingToolbar(
    items: List<FloatingMenuItem>,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    submenuPosition: SubmenuPosition = SubmenuPosition.Above,
    horizontalColors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    verticalColors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    horizontalContentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    verticalContentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    verticalMenuSpacing: Dp = 8.dp,
    dismissOnChildClick: Boolean = true,
    controlledActiveId: String? = null,
    onActiveIdChange: ((String?) -> Unit)? = null,
    onItemClick: ((FloatingMenuItem) -> Unit)? = null,
    onChildClick: ((parent: FloatingMenuItem, child: FloatingMenuItem) -> Unit)? = null,
) {
    var internalActiveId by remember { mutableStateOf<String?>(null) }
    val activeId = controlledActiveId ?: internalActiveId
    val setActive: (String?) -> Unit = { next ->
        if (controlledActiveId == null) internalActiveId = next
        onActiveIdChange?.invoke(next)
    }

    val itemCentersPx = remember { mutableStateMapOf<String, Float>() }
    var horizontalCenterPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val activeItem = items.firstOrNull { it.id == activeId }

    val submenu: @Composable () -> Unit = {
        val targetItem = activeItem?.takeIf { it.children.isNotEmpty() }
        val alignmentHint = if (submenuPosition == SubmenuPosition.Above)
            Alignment.Bottom else Alignment.Top
        val enter = FloatingToolbarDefaults.verticalEnterTransition(alignmentHint) + fadeIn()
        val exit = FloatingToolbarDefaults.verticalExitTransition(alignmentHint) + fadeOut()
        AnimatedContent(
            targetState = targetItem,
            contentKey = { it?.id },
            contentAlignment = if (submenuPosition == SubmenuPosition.Above)
                Alignment.BottomCenter else Alignment.TopCenter,
            transitionSpec = {
                enter.togetherWith(exit).using(SizeTransform(clip = false))
            },
            label = "expandable-toolbar-submenu",
        ) { current ->
            if (current == null) {
                Box(
                    modifier = Modifier.padding(
                        top = if (submenuPosition == SubmenuPosition.Below) verticalMenuSpacing else 0.dp,
                        bottom = if (submenuPosition == SubmenuPosition.Above) verticalMenuSpacing else 0.dp,
                    ),
                )
                return@AnimatedContent
            }
            val itemCenter = itemCentersPx[current.id]
            val offsetX: Dp = if (itemCenter != null && horizontalCenterPx > 0f) {
                with(density) { (itemCenter - horizontalCenterPx).toDp() }
            } else 0.dp

            VerticalFloatingToolbar(
                expanded = true,
                colors = verticalColors,
                contentPadding = verticalContentPadding,
                modifier = Modifier
                    .offset(x = offsetX)
                    .padding(
                        top = if (submenuPosition == SubmenuPosition.Below) verticalMenuSpacing else 0.dp,
                        bottom = if (submenuPosition == SubmenuPosition.Above) verticalMenuSpacing else 0.dp,
                    ),
                content = {
                    current.children.forEach { child ->
                        IconButton(onClick = {
                            child.onClick?.invoke()
                            onChildClick?.invoke(current, child)
                            if (dismissOnChildClick) setActive(null)
                        }) {
                            child.icon()
                        }
                    }
                },
            )
        }
    }

    val horizontal: @Composable () -> Unit = {
        HorizontalFloatingToolbar(
            expanded = expanded,
            colors = horizontalColors,
            contentPadding = horizontalContentPadding,
            modifier = Modifier.onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                horizontalCenterPx = pos.x + coords.size.width / 2f
            },
            content = {
                items.forEach { item ->
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            itemCentersPx[item.id] = pos.x + coords.size.width / 2f
                        },
                    ) {
                        IconButton(
                            onClick = {
                                item.onClick?.invoke()
                                onItemClick?.invoke(item)
                                if (item.children.isEmpty()) {
                                    setActive(null)
                                } else {
                                    setActive(if (activeId == item.id) null else item.id)
                                }
                            },
                        ) {
                            item.icon()
                        }
                    }
                }
            },
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (submenuPosition == SubmenuPosition.Above) {
            submenu()
            horizontal()
        } else {
            horizontal()
            submenu()
        }
    }
}

@Composable
fun ExpandableFloatingToolbarDemo(modifier: Modifier = Modifier) {
    var lastPicked by remember { mutableStateOf<String?>(null) }

    val items = remember {
        listOf(
            FloatingMenuItem(
                id = "edit",
                icon = { Icon(Icons.Filled.Edit, contentDescription = "Edit") },
                children = listOf(
                    FloatingMenuItem("edit-add", { Icon(Icons.Filled.Add, "Add") }),
                    FloatingMenuItem("edit-person", { Icon(Icons.Filled.Person, "Person") }),
                    FloatingMenuItem("edit-fav", { Icon(Icons.Filled.Favorite, "Favorite") }),
                ),
            ),
            FloatingMenuItem(
                id = "more",
                icon = { Icon(Icons.Filled.MoreVert, contentDescription = "More") },
                children = listOf(
                    FloatingMenuItem("more-i", { Icon(Icons.Filled.Add, "i") }),
                    FloatingMenuItem("more-j", { Icon(Icons.Filled.Person, "j") }),
                    FloatingMenuItem("more-k", { Icon(Icons.Filled.Favorite, "k") }),
                    FloatingMenuItem("more-l", { Icon(Icons.Filled.ArrowDropDown, "l") }),
                ),
            ),
            FloatingMenuItem(
                id = "fav",
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") },
                children = listOf(
                    FloatingMenuItem("fav-x", { Icon(Icons.Filled.Add, "x") }),
                    FloatingMenuItem("fav-y", { Icon(Icons.Filled.Person, "y") }),
                ),
            ),
            FloatingMenuItem(
                id = "leaf",
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                onClick = { lastPicked = "leaf (no sub-menu)" },
            ),
            FloatingMenuItem(
                id = "leaf",
                icon = { Icon(Icons.Filled.HomeMini, contentDescription = "Add") },
                onClick = null,
            ),
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = lastPicked?.let { "Last picked: $it" } ?: "Tap a parent icon",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )

            ExpandableFloatingToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                items = items,
                horizontalColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                verticalColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                verticalMenuSpacing = 12.dp,
                onChildClick = { parent, child ->
                    lastPicked = "${parent.id} → ${child.id}"
                },
            )
        }
    }
}
