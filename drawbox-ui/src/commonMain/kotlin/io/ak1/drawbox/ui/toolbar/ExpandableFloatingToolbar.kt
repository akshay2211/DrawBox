@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawbox.ui.toolbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One slot in [ExpandableFloatingToolbar]. Top-level items become the buttons
 * in the horizontal bar; nested [children] (if any) become the vertical sub-bar
 * shown above (or below — see [SubmenuPosition]) the item when it is active.
 *
 * @property isActive when true, the slot renders with a tonal accent
 *   background. For items that have *both* children and a non-null
 *   [onClick], being active also flips click semantics: clicking opens the
 *   submenu instead of firing [onClick]. Inactive items with the same shape
 *   fire [onClick] (e.g., "select this tool") without opening the submenu.
 *   Items with children but no [onClick] ignore [isActive] for click behavior
 *   and always open the submenu on tap.
 */
data class FloatingMenuItem(
    val id: String,
    val icon: @Composable (isActive: Boolean) -> Unit = { _ -> },
    val children: List<FloatingMenuItem> = emptyList(),
    val onClick: (() -> Unit)? = null,
    val isActive: Boolean = false,
    /**
     * Renders as a thin vertical divider instead of a button. Non-clickable;
     * [icon], [children], [onClick], and [isActive] are ignored. Use to group
     * related items in the bar.
     */
    val isSeparator: Boolean = false,
)

/** Convenience factory for a non-clickable visual separator slot. */
fun separator(id: String): FloatingMenuItem = FloatingMenuItem(id = id, isSeparator = true)

/**
 * Direction the vertical submenu pops in. [Above] suits bottom-anchored bars,
 * [Below] suits top-anchored bars like the contextual config bar.
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
 * @param activeItemBackground when true, items whose [FloatingMenuItem.isActive]
 *   is true render with a `primaryContainer` chip background so the selected
 *   tool reads at a glance. Also applied to selected submenu children.
 */
@Composable
fun ExpandableFloatingToolbar(
    items: List<FloatingMenuItem>,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    submenuPosition: SubmenuPosition = SubmenuPosition.Above,
    horizontalColors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    verticalColors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    horizontalContentPadding: PaddingValues = PaddingValues(2.dp),
    verticalContentPadding: PaddingValues = PaddingValues(2.dp),
    verticalMenuSpacing: Dp = 8.dp,
    dismissOnChildClick: Boolean = true,
    activeItemBackground: Boolean = true,
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
                    )
                    .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                    .requiredWidth(40.dp),
                content = {
                    current.children.forEach { child ->
                        if (child.isSeparator) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .requiredWidth(20.dp)
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            return@forEach
                        }
                        IconButton(
                            onClick = {
                                child.onClick?.invoke()
                                onChildClick?.invoke(current, child)
                                if (dismissOnChildClick) setActive(null)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .activeChipBg(child.isActive && activeItemBackground),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center,
                                    propagateMinConstraints = true,
                                ) {
                                    child.icon(child.isActive)
                                }
                            }
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
            modifier = Modifier
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .requiredHeight(40.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    horizontalCenterPx = pos.x + coords.size.width / 2f
                },
            content = {
                items.forEach { item ->
                    if (item.isSeparator) {
                        VerticalDivider(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .height(20.dp)
                                .padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        return@forEach
                    }
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            itemCentersPx[item.id] = pos.x + coords.size.width / 2f
                        },
                    ) {
                        IconButton(
                            onClick = {
                                val hasChildren = item.children.isNotEmpty()
                                val click = item.onClick
                                // Open submenu when:
                                //  - the item has children AND is already active (second tap on
                                //    a selected tool opens its sub-options), OR
                                //  - the item has children but no onClick (items whose only
                                //    purpose is to host a submenu).
                                val openSubmenu = hasChildren && (item.isActive || click == null)
                                if (!openSubmenu && click != null) click()
                                onItemClick?.invoke(item)
                                if (openSubmenu) {
                                    setActive(if (activeId == item.id) null else item.id)
                                } else {
                                    setActive(null)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .activeChipBg(item.isActive && activeItemBackground),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center,
                                    propagateMinConstraints = true,
                                ) {
                                    item.icon(item.isActive)
                                }
                            }
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

/**
 * Convenience: `MaterialTheme.colorScheme.primary` when true, otherwise the
 * ambient content colour. Used by every stock item-icon composable so that
 * "active" tinting is uniform across bars.
 */
@Composable
fun Boolean.activeIconTint(): Color =
    if (this) MaterialTheme.colorScheme.primary else LocalContentColor.current

/**
 * Applies a `primaryContainer` background clipped to [CircleShape] when
 * [active] is true. Sized to match the IconButton's state-layer (hover / press)
 * disc so the selected chip and the hover highlight share visual weight rather
 * than competing.
 */
@Composable
private fun Modifier.activeChipBg(active: Boolean): Modifier =
    if (active) this.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else this
