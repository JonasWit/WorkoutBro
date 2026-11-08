package com.wtk_apps.workoutbro.ui

import com.wtk_apps.workoutbro.data.TrainingPreset
import com.wtk_apps.workoutbro.data.WorkoutStore
import com.wtk_apps.workoutbro.timer.TimerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory store for UI and view-model tests: no DataStore, no Android, no disk. */
class FakeWorkoutStore(
  initialConfig: TimerConfig = TimerConfig(),
  initialPresets: List<TrainingPreset> = emptyList(),
  private val capacity: Int = Int.MAX_VALUE,
) : WorkoutStore {
  private val configState = MutableStateFlow(initialConfig)
  override val config: Flow<TimerConfig> = configState

  val presetsState = MutableStateFlow(initialPresets)
  override val presets: Flow<List<TrainingPreset>> = presetsState

  var savedConfig: TimerConfig? = null
  var deletedIndex: Int? = null

  override suspend fun save(config: TimerConfig) {
    savedConfig = config
    configState.value = config
  }

  override suspend fun addPreset(preset: TrainingPreset): Boolean {
    if (presetsState.value.size >= capacity) return false
    presetsState.value = presetsState.value + preset
    return true
  }

  override suspend fun updatePreset(
    index: Int,
    preset: TrainingPreset,
  ) {
    presetsState.value = presetsState.value.toMutableList().also { it[index] = preset }
  }

  override suspend fun deletePreset(index: Int) {
    deletedIndex = index
    presetsState.value = presetsState.value.filterIndexed { position, _ -> position != index }
  }
}
