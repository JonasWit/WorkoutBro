package com.wtk_apps.workoutbro.data

import com.wtk_apps.workoutbro.timer.TimerConfig

/** A named, saved training configuration. */
data class TrainingPreset(
  val name: String,
  val config: TimerConfig,
)

/** Upper bound on how many presets can be stored. */
const val MAX_PRESETS = 20
