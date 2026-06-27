package io.ak1.drawbox.text

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.TextAlignment
import io.ak1.drawbox.domain.model.Viewport

/**
 * Built-in inline text editor for [Element.Text]. Renders as the element
 * it edits — same font family, font size, alignment, color, wrap width, and
 * position — so the caret is the only visible "you are editing" affordance.
 * The element underneath is expected to be hidden by the host via
 * [io.ak1.drawbox.DrawBox]'s `hiddenElementIds` parameter to avoid ghosting.
 *
 * Embedders are free to ignore this composable and ship their own editor
 * (modal, side-panel, IME-driven, etc.) — the SDK's `InsertText` /
 * `UpdateText` intents and `Element.Text` model don't depend on it. This
 * exists as the "drop-in" path for hosts that don't want to write one.
 *
 * ### Pixel fidelity with the renderer
 *
 * The implementation deliberately mirrors the SDK's text rendering path:
 *
 * - `TextStyle.fontSize` is `(element.fontSize × viewport.scale).sp`
 *   *directly* — NOT `.toSp()`. `Density.toSp(Float)` treats its input as
 *   dp-equivalent and divides by `fontScale`; feeding it screen pixels
 *   would shrink the editor by `1 / fontScale` whenever the system text
 *   scale is non-unit. Going `.sp` direct routes editor and renderer
 *   through the same final conversion (`fontSize × density × fontScale`),
 *   so on-screen pixels match across any density / fontScale /
 *   viewport.scale combination.
 * - `width = element.wrapWidth × viewport.scale` (no `fillMaxWidth`) — the
 *   field wraps at the same point as `TextMeasurer.measure(constraints =
 *   Constraints(maxWidth = wrapWidth))`.
 * - `lineHeight` is left unspecified in both editor and renderer so line
 *   breaks land identically.
 * - Position via `Modifier.offset { IntOffset(...) }` re-reads
 *   [Viewport.worldToScreen] each composition — pan / zoom while editing
 *   reposition smoothly.
 *
 * ### State
 *
 * [draft] / [onDraftChange] are hoisted so the host's outside-tap overlay
 * can read the current draft when committing, without coordinating focus
 * events. Recommended host flow:
 *
 * 1. When the user enters TEXT mode and taps, the SDK dispatches
 *    `Intent.InsertText("")` from the canvas tap handler.
 * 2. Host detects a new empty `Element.Text` (e.g. via a
 *    `LaunchedEffect(state.elements)`), records its id as `editingId`,
 *    seeds `draft = ""`.
 * 3. Host passes `hiddenElementIds = setOf(editingId)` to `DrawBox`.
 * 4. Host renders this composable + a full-screen click-catcher overlay.
 *    Tapping outside the field triggers commit (`updateText(id, draft)`
 *    if non-empty, else `Intent.DeleteElement(id)`).
 *
 * ### Font family
 *
 * The family is resolved internally from [FontRegistry] via
 * [Element.Text.fontFamilyKey]. Hosts that want a different family
 * register a new key on app launch and set
 * [io.ak1.drawbox.presentation.viewmodel.DrawBoxController.setFontFamily]
 * before the next insert.
 *
 * @param element the text element being edited.
 * @param viewport the current camera transform — used to project the
 *   element's world position into screen space and to scale font size.
 * @param draft current edit content. Hosted by the caller so a sibling
 *   "outside-tap commits" overlay can read the latest value at any time.
 * @param onDraftChange fires on every keystroke with the new draft.
 */
@Composable
fun InlineTextEditor(
    element: Element.Text,
    viewport: Viewport,
    draft: String,
    onDraftChange: (String) -> Unit,
) {
    val screenTopLeft = viewport.worldToScreen(element.topLeft)
    val screenWidth = element.wrapWidth * viewport.scale

    val widthDp = with(androidx.compose.ui.platform.LocalDensity.current) {
        screenWidth.toDp()
    }
    val fontSizeSp = (element.fontSize * viewport.scale).sp

    val focusRequester = remember(element.id) { FocusRequester() }
    LaunchedEffect(element.id) {
        runCatching { focusRequester.requestFocus() }
    }

    BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        textStyle = TextStyle(
            fontSize = fontSizeSp,
            fontFamily = FontRegistry.resolve(element.fontFamilyKey),
            textAlign = when (element.alignment) {
                TextAlignment.LEFT -> TextAlign.Left
                TextAlignment.CENTER -> TextAlign.Center
                TextAlignment.RIGHT -> TextAlign.Right
            },
            color = element.color,
        ),
        cursorBrush = SolidColor(element.color),
        modifier = Modifier
            // Editor sits in the topmost compositional layer of the host
            // tree — `zIndex(10f)` puts it above the recommended click-
            // catcher overlay (z 9) and far above the canvas (z 0).
            .zIndex(10f)
            .offset {
                IntOffset(screenTopLeft.x.toInt(), screenTopLeft.y.toInt())
            }
            .width(widthDp)
            .focusRequester(focusRequester),
    )
}
