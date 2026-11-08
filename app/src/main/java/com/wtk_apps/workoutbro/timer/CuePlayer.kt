package com.wtk_apps.workoutbro.timer

/** Receives [TimerCue]s from the controller and turns them into sound/vibration. */
interface CuePlayer {
  fun play(
    cue: TimerCue,
    config: TimerConfig,
  )
}
