<!--
Maintainer response policy: PRs receive a first response within 7 days.
For larger changes (new public API, new module, new target), please open a short RFC in docs/rfcs/ first and link it below.
-->

## Summary
<!-- What does this change and why? One or two sentences is fine. -->

## Type of change
- [ ] Bug fix (non-breaking)
- [ ] New feature (non-breaking, additive to the frozen 2.x surface)
- [ ] Incubating API (opt-in via annotation)
- [ ] Breaking change (would land in a future major)
- [ ] Docs / build / CI only

## Affected targets
- [ ] Android
- [ ] iOS
- [ ] JVM Desktop
- [ ] Web (WASM)
- [ ] Web (Kotlin/JS)

## Public API impact
- [ ] No public API change
- [ ] Additive only (new symbols, no signature changes)
- [ ] Modifies a `2.x` frozen surface — link to RFC or discussion: <!-- url -->

## Testing
<!-- Which tests cover this? Snapshot tests? Manual reproduction? -->
- [ ] Unit tests added / updated
- [ ] Snapshot tests updated (Roborazzi baselines regenerated if applicable)
- [ ] Manually verified on: <!-- e.g. Android emulator API 34, macOS Desktop, iOS simulator, Chrome WASM -->

## Checks
- [ ] `./gradlew :DrawBox:spotlessCheck` passes
- [ ] `./gradlew :DrawBox:jvmTest` passes
- [ ] `./gradlew :DrawBox:dokkaGenerateHtml` passes
- [ ] CHANGELOG entry added under the correct release heading (if user-facing)
- [ ] Migration notes added (if the JSON scene format or a frozen surface moved)

## Screenshots / recording
<!-- For UI-visible changes in the sample app or drawbox-ui module. -->

## Linked issues
<!-- e.g. Closes #123, Refs #456 -->