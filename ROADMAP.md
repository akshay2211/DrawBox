# DrawBox Roadmap

This roadmap describes the direction of DrawBox over the next 12–18 months. It is intended to give users, contributors, and partners a clear view of where the library is headed, why, and how to get involved.

The roadmap is a living document. Dates are targets, not commitments — major shifts are reflected in GitHub Milestones and announced in release notes.

## Vision

DrawBox aims to be **the canonical drawing primitive for Compose Multiplatform** — a small, focused, accessible, and well-benchmarked canvas layer that any annotation, whiteboard, diagramming, or design tool can build on, across Android, iOS, Web (Wasm), and Desktop.

We prioritize **primitives over products**: a strong foundation other tools depend on, rather than a feature-rich app.

## Status

Latest published version: **2.0.0-alpha02** (KMP preview)

The 2.x line is a full rewrite on Kotlin Multiplatform with shared drawing logic and an MVI architecture. The current alpha is feature-complete for the core drawing primitives across all four target platforms and is in stabilization.

## Near term — 2.0.0 stable (Q3 2026)

Goal: ship 2.0.0 with a stable public API and a documented migration path from 1.x.

- API freeze for `DrawBoxController`, `Mode`, `Intent`, and `Event`
- Migration guide from 1.x (Android-only) to 2.x (KMP)
- Public WASM playground at `akshay2211.github.io/DrawBox/sample/`
- Snapshot-based regression tests across platforms
- Dokka API reference published alongside the user docs

## Next — Accessibility & performance (Q4 2026)

Goal: make DrawBox the first KMP drawing library with serious accessibility and measurable cross-platform performance.

- **Accessibility pass**
  - Semantic descriptions for the canvas, tool controls, and exported drawings
  - Keyboard-only drawing and tool selection
  - Screen reader support on Android (TalkBack), iOS (VoiceOver), and Web
- **Performance benchmarks**
  - Frame time, gesture latency, and memory profile published per platform
  - Reproducible benchmark harness in `benchmarks/`
  - Public baselines so regressions are visible

## Mid term — Extensibility (Q1 2027)

Goal: let third parties extend DrawBox without forking it.

- Pluggable tool API — community-contributed `Mode`s as separate artifacts
- Pluggable exporters — beyond SVG/PNG/JSON (e.g. PDF, DXF)
- Stable extension contracts with semantic versioning
- A first example community plugin to validate the surface

## Long term — Collaboration & domain integrations (2027 H2)

Goal: unlock real-time and domain-specific use cases.

- CRDT-based collaborative drawing primitive
- PDF annotation surface
- Reference integrations with adjacent Compose libraries (e.g. rich text, slides)

## Continuous

Work that runs in parallel across all phases:

- Triage and respond to issues within 7 days
- Track JetBrains Compose Multiplatform releases and bug-report blocker findings upstream
- Maintain `CONTRIBUTING.md`, `good first issue` triage, and quarterly release cadence
- Publish a release blog post for every minor version

## How to get involved

- **Try it**: the live WASM sample is at `https://akshay2211.github.io/DrawBox/sample/`
- **Report**: open issues on [GitHub](https://github.com/akshay2211/DrawBox/issues) — bug reports, feature requests, and accessibility findings are all welcome
- **Contribute**: see [CONTRIBUTING](docs/development/contributing.md). Start with issues tagged `good first issue`
- **Propose**: larger changes go through a short RFC in `docs/rfcs/` before implementation
- **Sponsor**: see the repository sidebar for sponsorship links

## Out of scope

To keep the library small and focused, the following are explicitly *not* on the roadmap:

- A full vector editor UI — DrawBox stays a primitive, not a product
- Server-side rendering of drawings
- Native rendering backends other than Compose

If your use case needs one of these, please open a discussion — we may be able to support it via the extensibility surface above.