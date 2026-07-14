# DrawBox RFCs

An RFC (Request for Comments) is a short design document used to discuss non-trivial changes to DrawBox before implementation. This directory holds accepted, rejected, and in-progress RFCs.

## When an RFC is required

Open an RFC when the change would:

- Add, remove, or rename anything on the frozen `2.0` public surface (`DrawBoxController`, `Mode`, `Intent`, `Event`, `State`).
- Introduce a new module (e.g. `drawbox-ai`) or a new published artifact.
- Change the JSON scene format or its migration contract.
- Alter export semantics (SVG, PNG, JSON) in a way that affects existing consumers.
- Introduce a new external dependency to the `DrawBox` module, or a new supported platform target.

An RFC is **not** required for:

- Bug fixes, performance work, or documentation-only changes.
- Additive, opt-in APIs behind an incubating annotation.
- Internal refactors that keep the public surface stable.
- Anything already tracked as a good-first-issue or a scoped enhancement.

If you are unsure, open a GitHub Discussion or issue first — a maintainer will tell you whether an RFC is needed.

## Process

1. **Draft** — copy [`0000-template.md`](0000-template.md) to `NNNN-short-title.md` (leave `NNNN` as `0000` for the draft PR; a number is assigned on merge).
2. **Open a pull request** against `main` that adds the RFC file only. The PR description should link to any related issues.
3. **Discuss** — the RFC PR is the discussion venue. Expect maintainer feedback within the 7-day response SLO. Substantive concerns and their resolution should be visible in the PR history so future readers can trace the decision.
4. **Decision** — the RFC is merged (accepted), closed (rejected), or paused (superseded / needs-info). Rejected RFCs are still merged with `Status: Rejected` when the discussion is valuable for future readers.
5. **Implementation** — accepted RFCs are tracked by a matching issue or milestone. The RFC file is updated with the target release and a link to the tracking issue.

## Numbering

- Numbers are assigned in merge order. Draft under `0000-*` and a maintainer will rename the file at merge time.
- Numbers are never reused; withdrawn RFCs keep their number and are marked `Status: Withdrawn`.

## Index

- [0001 — Text elements and in-place editing](0001-text-elements.md) — Accepted (retroactive). Tracks #74 / #83.

_Next candidates on the roadmap: the pluggable-tool API and the `DrawBoxAi` module — see [ROADMAP.md](../../ROADMAP.md)._