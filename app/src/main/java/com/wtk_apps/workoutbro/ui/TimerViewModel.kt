package com.wtk_apps.workoutbro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wtk_apps.workoutbro.AppGraph
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import com.wtk_apps.workoutbro.timer.TimerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Seam between the Compose screens and the process-scoped [TimerController].
 *
 * It adds no behaviour: it exposes the controller state and forwards commands, so screens
 * never reach into the global graph directly. The factory is the single place where the
 * process-scoped instance is picked up.
 */
class TimerViewModel(
  private val controller: TimerController,
) : ViewModel() {
  val state: StateFlow<TimerState> = controller.state

  fun toggle() = controller.toggle()

  fun reset() = controller.reset()

  fun skip() = controller.skip()

  fun adjust(deltaMillis: Long) = controller.adjust(deltaMillis)

  fun configure(config: TimerConfig) = controller.configure(config)

  companion object {
    fun factory(): ViewModelProvider.Factory = viewModelFactory { initializer { TimerViewModel(AppGraph.timer) } }
  }
}
