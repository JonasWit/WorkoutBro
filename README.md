<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="112" alt="WorkoutBro icon">

<h1>WorkoutBro</h1>

<p><strong>Set the rounds. Press start. Follow the cues.</strong></p>

<p>An interval training timer for Android that stays readable from across the room,<br>
keeps running with the screen off, and remembers your workouts.</p>

<p>
  <a href="https://github.com/JonasWit/WorkoutBro/releases/latest"><img src="https://img.shields.io/github/v/release/JonasWit/WorkoutBro?style=flat-square&label=release&color=c9a227" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License: MIT"></a>
  <a href="#installation"><img src="https://img.shields.io/badge/Android-7.0%2B-3ddc84?style=flat-square&logo=android&logoColor=white" alt="Android 7.0+"></a>
</p>

<p><strong><a href="https://github.com/JonasWit/WorkoutBro/releases/latest">Download the latest APK</a></strong></p>

</div>

---

## Why WorkoutBro

- **Read it from across the room.** The countdown is large enough to follow from
  several meters away, so you do not have to pick up the phone mid-set.
- **Know the phase at a glance.** A progress ring and a session bar show where you
  are, while the flame background changes color and speed with each phase.
- **Hear and feel every change.** Sound and vibration mark the start of work, the
  end of a round, the half-way point and the end of the workout.
- **Keep training with the screen off.** A foreground service keeps the timer
  running, and the notification shows the time left.
- **Reuse your workouts.** Save up to 20 named configurations, load one in a tap,
  and back them all up to a single JSON file.
- **Keep your data.** No account, no login, no network permission — everything
  stays on the phone.

## Features

### Interval timer

- phases: prepare, work, rest and finish, with automatic transition to the next round
- configurable number of rounds and work, rest and prepare durations
- 3-2-1 countdown before each round, can be turned off
- half-way cue during the work round, can be turned off
- start, pause, resume, reset and skip phase
- quick 15-second extension and shortening of the current phase

### During a workout

- large countdown readable from several meters away
- progress ring for the current phase
- segmented bar for the whole session, split into prepare, work and rest
- animated flame background whose color and speed follow the phase
- sounds and vibration for work start, round end, half-way and workout end
- the screen stays on during a session
- the timer keeps running with the screen off, with a notification showing the remaining time

### Saved workouts

- save the current configuration as a named workout
- up to 20 saved workouts
- preview parameters, load, edit and delete
- export all workouts to a JSON file and import from a file
- import reports how many workouts were added and how many were skipped because of the limit

### Interface

- four screens: timer, settings, workout list and workout editor
- all user-facing text lives in the Android string resources
- version number shown under the title on the main screen

## The four screens

| Screen | What you do there |
| --- | --- |
| **Timer** | Start, pause, resume, reset or skip a phase, adjust the current phase by 15 seconds, and follow the round and session progress. |
| **Settings** | Set the rounds and the prepare, work and rest durations, choose the countdown and half-way cues, and toggle sound and vibration. |
| **Trainings** | Preview, load, edit or delete saved workouts, and export or import the JSON backup. |
| **Edit training** | Rename a saved workout and change its parameters. |

## Installation

1. Download `app-release.apk` from the
   [Releases](https://github.com/JonasWit/WorkoutBro/releases/latest) tab.
2. Open the file on your phone. If Android asks, allow installing from this source.

> **Updating from a build signed with a different key?** Android will refuse the
> update. Open `Trainings` → `EXPORT`, uninstall the old version, install the new
> one, then use `IMPORT` to restore your workouts.

> **Coming from WorkoutBase?** The rename changed the package id, so WorkoutBro
> installs next to the old app instead of replacing it. Open `Trainings` → `EXPORT`
> in WorkoutBase, install WorkoutBro, then use `IMPORT` there. You can remove the
> old app afterwards.

## Your first workout

1. Open `Settings`, set the rounds and the prepare, work and rest durations, then save.
2. Go back to the timer and press **START**.
3. To keep this setup, open `Trainings`, press **SAVE CURRENT WORKOUT** and give it a name.

## Data and privacy

All data stays on the device. The app has no account, no login and no network
permission, so nothing is uploaded. Workouts are stored locally, and the only way to
move them is a manual export to a JSON file.

## License

MIT — see [`LICENSE`](LICENSE) for details.

<div align="center"><sub>Built with Kotlin and Jetpack Compose.</sub></div>
