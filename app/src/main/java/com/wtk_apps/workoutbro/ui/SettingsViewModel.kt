package com.wtk_apps.workoutbro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wtk_apps.workoutbro.AppGraph
import com.wtk_apps.workoutbro.data.SettingsStore
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings screen state: the stored configuration plus the save command.
 *
 * Saving does two things in this order: persist, then apply to the running timer. Keeping
 * that order in one place prevents a screen from applying a configuration it failed to save.
 */
class SettingsViewModel(
  private val settings: SettingsStore,
  private val timer: TimerController,
) : ViewModel() {
  val config: StateFlow<TimerConfig> =
    settings.config
      .stateIn(viewModelScope, SharingStarted.Eagerly, timer.state.value.config)

  fun save(
    config: TimerConfig,
    onSaved: () -> Unit = {},
  ) {
    viewModelScope.launch {
      settings.save(config)
      timer.configure(config)
      onSaved()
    }
  }

  companion object {
    fun factory(): ViewModelProvider.Factory = viewModelFactory { initializer { SettingsViewModel(AppGraph.settings, AppGraph.timer) } }
  }
}
