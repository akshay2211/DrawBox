# Security Policy

## Supported Versions

| Version            | Status                          | Security fixes |
| ------------------ | ------------------------------- | -------------- |
| `2.0.x` (KMP)      | Active development              | Yes            |
| `1.x` (Android)    | Legacy, superseded by `2.0.x`   | Critical only  |

The active KMP line (`2.0.x`) is the recommended target for all new integrations. The Android-only `1.x` line is in maintenance mode.

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security reports.

Report vulnerabilities privately via GitHub's [Private Vulnerability Reporting](https://github.com/akshay2211/DrawBox/security/advisories/new) form, or by email to **fxn769@gmail.com** with the subject line `DrawBox security report`.

Please include:

- A description of the issue and its impact.
- Steps to reproduce, ideally a minimal sample project.
- The DrawBox version and platform target (Android, iOS, JVM, WASM).
- Any relevant logs, stack traces, or proof-of-concept code.

## Response Expectations

- **Acknowledgement:** within 7 days of report.
- **Triage and initial assessment:** within 14 days.
- **Fix or mitigation plan:** communicated within 30 days for confirmed issues.

Timelines may shift for complex issues; in that case the reporter will be kept informed.

## Disclosure Policy

DrawBox follows coordinated disclosure:

1. The reporter and maintainer agree on a fix and a disclosure date.
2. A patched release is published to Maven Central and tagged on GitHub.
3. A GitHub security advisory is published describing the issue, affected versions, and the fix.

Credit is given to reporters who request it.

## Scope

In scope:

- The published `io.ak1:drawbox` library artifacts.
- Sample applications in this repository (`androidApp`, `iosApp`, `desktopApp`, `webApp`) when the issue stems from library code.
- Build, release, and CI configuration that affects what ships to consumers.

Out of scope:

- Vulnerabilities in third-party dependencies — please report those upstream. We will track and update affected dependencies once fixes are available.
- Issues that require an attacker to already have arbitrary code execution on the host application.
