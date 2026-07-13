---
Title: <short descriptive title>
Author: <your GitHub handle>
Status: Draft
Created: <YYYY-MM-DD>
Target release: <e.g. 2.2.0, or "unassigned">
Tracking issue: <link, or "none yet">
---

# Summary

One paragraph. What is being proposed, in plain language.

# Motivation

What problem does this solve? Who feels the pain today? Why is the status quo insufficient? Point at real issues, integrations, or user reports where possible.

# Design

The concrete proposal. Public API sketches, data-model changes, or module boundaries as appropriate. Include Kotlin signatures for anything that lands on the public surface.

# Public API impact

- Which frozen `2.0` surfaces are touched, if any?
- Are new surfaces additive, or do they require a semver-major bump?
- What annotations (e.g. incubating, experimental) do the new surfaces need?

# Migration and rollout

- Behaviour on existing scenes / JSON payloads.
- Deprecation path for anything being replaced.
- Feature-flag / opt-in story, if the change should ship dark first.

# Alternatives considered

At least two. For each: what would it look like, and why is the chosen design better?

# Prior art

Links to how other Compose Multiplatform, drawing, or annotation libraries handle the same problem. Learning from precedent is expected; copying without attribution is not.

# Unresolved questions

Anything the RFC intentionally leaves open for discussion in the PR thread.