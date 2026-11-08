package com.wtk_apps.workoutbro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wtk_apps.workoutbro.AppGraph
import com.wtk_apps.workoutbro.data.TrainingPreset
import com.wtk_apps.workoutbro.data.WorkoutStore
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Saved trainings: list, save the current configuration, edit, delete and apply.
 *
 * "Save current" reads the active configuration from the timer at the moment of the tap, so
 * the screen does not have to know the controller at all.
 */
class PresetsViewModel(
  private val store: WorkoutStore,
  private val timer: TimerController,
) : ViewModel() {
  val presets: StateFlow<List<TrainingPreset>> =
    store.presets
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  fun saveCurrent(
    name: String,
    onResult: (Boolean) -> Unit = {},
  ) {
    viewModelScope.launch {
      onResult(store.addPreset(TrainingPreset(name, timer.state.value.config)))
    }
  }

  fun update(
    index: Int,
    name: String,
    config: TimerConfig,
  ) {
    viewModelScope.launch { store.updatePreset(index, TrainingPreset(name, config)) }
  }

  fun delete(index: Int) {
    viewModelScope.launch { store.deletePreset(index) }
  }

  /** Appends imported presets until the store is full; reports what was taken. */
  fun importPresets(
    imported: List<TrainingPreset>,
    onResult: (ImportResult) -> Unit = {},
  ) {
    viewModelScope.launch {
      var added = 0
      var skipped = 0
      for (preset in imported) {
        if (store.addPreset(preset)) added++ else skipped++
      }
      onResult(ImportResult(added = added, skipped = skipped))
    }
  }

  fun apply(
    preset: TrainingPreset,
    onApplied: () -> Unit = {},
  ) {
    viewModelScope.launch {
      store.save(preset.config)
      timer.configure(preset.config)
      onApplied()
    }
  }

  companion object {
    fun factory(): ViewModelProvider.Factory = viewModelFactory { initializer { PresetsViewModel(AppGraph.settings, AppGraph.timer) } }
  }
}

/** Outcome of an import, so the screen can tell the user what happened. */
data class ImportResult(
  val added: Int,
  val skipped: Int,
)
