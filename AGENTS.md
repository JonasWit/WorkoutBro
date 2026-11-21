# Agent Instructions

Android app (Kotlin, Jetpack Compose, single module `app`). The canonical coding rules
live in the knowledge repository:
<https://github.com/JonasWit/ai_stuff/blob/main/docs/zasady-ai.md>.
Read it before writing code. This file only records what is specific to this project.

## Verification

- Tests come first, always. The only exceptions are changes with no executable
  behavior: documentation, comments, commit messages, tool-enforced formatting.
- Watch the test fail before implementing. A test that passed before the change
  proves nothing.
- No claim without a test; otherwise say "unverified" and what is missing.
- Commands:
  - fast loop while iterating: `./gradlew fastTest --offline` (unit tests only)
  - unit tests: `./gradlew :app:testDebugUnitTest`
  - full check (lint + unit tests): `./gradlew check`
- Every commit message needs a trailer line: `Test: <name>` or
  `Unverified: <reason>`. The `commit-msg` hook rejects anything else.
  Enable it once per clone: `git config core.hooksPath .githooks`

## Structure

- `timer/TimerEngine.kt`, `timer/TimerFormat.kt` — pure core: no Android imports, time
  passed in as a parameter, immutable state.
- `timer/TimerController.kt` — drives the engine with wall-clock time; app-scoped.
- `timer/AndroidCuePlayer.kt` — adapter for sound and vibration behind `CuePlayer`.
- `data/` — DataStore persistence for `TimerConfig` and presets.
- `ui/` — Compose screens.
- `service/` — foreground service and notification.

## Known debt (do not fix unless the task touches it)

- Instrumented tests require a device that allows installation; on MIUI this needs
  "Install via USB" enabled in developer options.

Fixed and removed from this list: `TimerController` now takes an injected clock,
serializes state transitions, and has tests; configuration mapping lives in
`ConfigPreferences`; duration conversion lives in `DurationParts.kt`; cue behaviour lives
in `cuePlan` with tests; user-facing text lives in `strings.xml`; screens go through
`TimerViewModel`, `SettingsViewModel`, and `PresetsViewModel` and are split into
topic-sized files; the detekt baseline has been eliminated (zero issues).

## Boundaries

- One change, one purpose. Report unrelated problems instead of fixing them in the
  same diff.
- New dependency needs a justification: what the standard library or Compose cannot do.
- Versioning: bump `versionCode` by one and `versionName` (`major.minor.patch`) in every change commit, before committing. A change without a bump is incomplete.
- Do not guess Android APIs. Check the version in `gradle/libs.versions.toml` first.
- Never run instrumented tests against a phone that holds real user data:
  `connectedDebugAndroidTest` uninstalls the app and wipes its DataStore. Use the
  emulator.
