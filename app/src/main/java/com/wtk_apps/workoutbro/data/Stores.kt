package com.wtk_apps.workoutbro.data

import com.wtk_apps.workoutbro.timer.TimerConfig
import kotlinx.coroutines.flow.Flow

/**
 * Port for the active configuration. Narrow on purpose: screens need to read and save it,
 * nothing else, which keeps view models testable without Android or DataStore.
 */
interface SettingsStore {
  val config: Flow<TimerConfig>

  suspend fun save(config: TimerConfig)
}

/** Port for saved trainings. */
interface PresetsStore {
  val presets: Flow<List<TrainingPreset>>

  suspend fun addPreset(preset: TrainingPreset): Boolean

  suspend fun updatePreset(
    index: Int,
    preset: TrainingPreset,
  )

  suspend fun deletePreset(index: Int)
}

/** Both ports together: what a training screen needs from persistence. */
interface WorkoutStore :
  SettingsStore,
  PresetsStore
