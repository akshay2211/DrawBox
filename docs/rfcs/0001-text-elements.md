---
Title: Text elements and in-place editing
Author: akshay2211
Status: Accepted (retroactive)
Created: 2026-07-13
Target release: 2.2.0
Tracking issue: https://github.com/akshay2211/DrawBox/issues/83
---

# Summary

Adds first-class text to DrawBox: an `Element.Text` model, the intents and
controller methods to create and restyle it, a font-family registry, a drop-in
`InlineTextEditor`, and an `Event.TextEditRequested` signal for host-driven
in-place editing (double-tap / re-tap-to-edit). This RFC is written
retroactively: the `Element.Text` surface shipped in `2.1.0-alpha01` before the
RFC process existed, and the editing follow-ups (#83) land in `2.2.0`. It exists
to make the frozen-surface additions reviewable in one place, as required by
`docs/rfcs/README.md`.

# Motivation

Drawing tools without text are half a tool — annotations, labels, and callouts
are among the most-requested primitives. Issue #74 specced the element; #83
tracked the editing polish (mid-edit styling, double-tap, multi-select merge,
commit semantics). The status quo (shapes + freehand only) forced embedders to
overlay their own text layer and reconcile it with export, selection, and undo
themselves.

# Design

## Model (already shipped, `2.1.0-alpha01`)

`Element.Text` carries `text`, `topLeft`, `fontSize`, `wrapWidth`,
`fontFamilyKey` (a `String` resolved at render time, never a bundled asset),
`alignment`, and `color`.

## Intents / controller (frozen surface additions)

```kotlin
// Intent
data class InsertText(text, position, fontSize, fontFamilyKey, alignment, color)
data class UpdateText(id, text)
data class SetSelectedFontSize(size)         // applies to every selected Text
data class SetSelectedTextAlignment(alignment)
data class SetSelectedFontFamily(fontFamilyKey)
data class RequestTextEditAt(offset, tolerance = 8f)   // new in 2.2.0 (#83.2)

// DrawBoxController
fun insertText(...); fun updateText(id, text)
fun registerFont(key: String, family: FontFamily)
fun setSelectionFontSize/TextAlignment/FontFamily(...)
```

## Event (frozen surface addition, `2.2.0`)

```kotlin
data class TextEditRequested(val id: String) : Event()
```

Emitted by the controller when the user double-taps a text in `SELECT` mode, or
taps an already-sole-selected text again. The reducer selects the hit element;
the controller resolves the pick (`Reducer.hitTopmost`) and emits the event. The
host opens whatever editor it likes; `InlineTextEditor` is the provided default.

## Fonts

`FontRegistry` maps `String` keys to Compose `FontFamily`. Three built-ins
(`sans`/`serif`/`mono`) are pre-registered to platform generics; hosts register
real faces at launch. On web, Skia-WASM collapses generics to one face, so a
one-shot dev warning fires — see #89 and `FontRegistry` KDoc.

# Public API impact

- Frozen `2.0` surfaces touched: `Intent`, `Event`, `DrawBoxController` — all
  **additive** (new sealed subclasses / new methods). No signatures changed or
  removed, so this is a minor (`2.2.0`), not a major, bump.
- `Element` gains the `Text` subclass — additive to the sealed hierarchy;
  exhaustive `when` consumers outside the SDK must add a branch (source-compatible
  only with an `else`). Called out here because sealed-hierarchy growth is the one
  non-obvious semver wrinkle.
- Annotations: none. We considered gating the text surface behind an incubating
  annotation but chose not to (see Alternatives) — the surface is small, already
  shipped, and we are willing to keep it stable.

# Migration and rollout

- Existing scenes / JSON: unaffected. Scenes without text deserialize unchanged;
  the `Text` variant is additive to the JSON element union.
- No deprecations — nothing is being replaced.
- No feature flag. The editing follow-ups are sample-app behavior plus the one
  additive `RequestTextEditAt`/`TextEditRequested` pair; hosts that ignore the
  event keep their existing flow.

# Alternatives considered

1. **Gate the text API behind an `@Incubating` annotation.** Would have satisfied
   the RFC escape hatch cleanly and bought room to break the surface. Rejected:
   the annotation doesn't yet exist in the codebase (a separate hygiene item),
   the text surface already shipped un-annotated in `2.1.0-alpha01`, and
   retroactively marking a released API incubating is more confusing than
   committing to it. A real incubating-annotation strategy is tracked separately.
2. **A `DrawBox` callback param (`onTextEditRequested: (String) -> Unit`) instead
   of an `Event`.** Rejected: it splits text-edit notification out of the single
   `Event` channel hosts already collect, and grows the composable's parameter
   list. Routing through `Event` keeps one notification path.

# Prior art

tldraw and Figma both use double-click-to-edit and second-click-on-selected as
the two ways into text editing; this RFC mirrors that. Jetpack Compose's own
`BasicTextField` backs `InlineTextEditor`. Font-key indirection (register a face
under a name, reference the name on the model) follows the Android
`FontFamily`/resource pattern rather than embedding font bytes in the scene.

# Unresolved questions

- Multi-text inline editing (typing into several elements at once) is out of
  scope; only style-merge (#83.3) is addressed.
- Configurable commit policy on `InlineTextEditor` (Esc-cancel / focus-loss are
  currently hardcoded host-side, #83.5) is deferred until an embedder needs it.
