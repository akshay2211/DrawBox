// Kotlin/JS and Kotlin/WASM browser tests run under Karma against headless
// Chrome. On Linux CI runners Chrome's setuid sandbox cannot initialize
// (ZygoteHostImpl fatal -> "cannot start"), so launch with --no-sandbox.
// Harmless on local macOS/Windows runs. The Kotlin Gradle plugin concatenates
// every file in this directory into the generated Karma config, where `config`
// is in scope.
config.set({
    browsers: ["ChromeHeadlessNoSandbox"],
    customLaunchers: {
        ChromeHeadlessNoSandbox: {
            base: "ChromeHeadless",
            flags: ["--no-sandbox"],
        },
    },
});