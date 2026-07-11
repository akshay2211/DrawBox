# Contributing to DrawBox

Thanks for your interest in contributing. DrawBox is an Apache-2.0 Kotlin Multiplatform SDK; contributions of any size are welcome — bug reports, small fixes, tests, docs, and larger features.

The full contributor guide lives in [`docs/contributing.md`](docs/contributing.md). This file is the short version so it surfaces in GitHub's PR/issue UI and in the community profile.

## Before you start

- Search [existing issues](https://github.com/akshay2211/DrawBox/issues) to check whether the problem or idea is already tracked.
- For non-trivial changes, open an issue first so the direction can be discussed before you invest time. Larger changes go through a short RFC in [`docs/rfcs/`](docs/rfcs/).
- Good starting points are tagged [`good first issue`](https://github.com/akshay2211/DrawBox/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22).

## Development setup

Requirements:

- JDK 21
- A recent Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin
- For iOS work: Xcode on macOS

Common commands:

```bash
./gradlew :DrawBox:build            # Build the library
./gradlew :DrawBox:jvmTest          # Run common + JVM tests
./gradlew :DrawBox:spotlessApply    # Auto-format sources
./gradlew :webApp:wasmJsBrowserDevelopmentRun   # Run the WASM sample
```

CI runs Spotless, JVM tests, WASM sample build, and an iOS compile check on every PR. See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Pull request checklist

- [ ] Branch cut from `main`, focused on a single change.
- [ ] `./gradlew :DrawBox:spotlessCheck` passes locally.
- [ ] `./gradlew :DrawBox:jvmTest` passes locally.
- [ ] Tests added or updated for behavioural changes.
- [ ] Public API changes are noted in [`CHANGELOG.md`](CHANGELOG.md) under the next unreleased version.
- [ ] Commit messages follow the `type: subject` convention (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `perf:`).

## Public API and stability

The `2.0` public surfaces (`DrawBoxController`, `Mode`, `Intent`, `Event`, `State`) are frozen under semver — they are extended only, not renamed or removed. Incubating surfaces are opt-in via explicit annotations. If a change would break the frozen surface, flag it explicitly in the PR description.

## Reporting bugs

Use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md). Include:

- DrawBox version and target platform (Android / iOS / JVM / WASM / JS).
- Steps to reproduce, ideally a minimal sample.
- Expected vs. actual behaviour, plus any logs or stack traces.

## Reporting security issues

Do **not** open a public issue for vulnerabilities. Follow [`SECURITY.md`](SECURITY.md) — report via GitHub Private Vulnerability Reporting or the private email listed there.

## Code of conduct

By participating, you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

DrawBox is licensed under Apache 2.0. By submitting a contribution you agree that your work will be released under the same license.