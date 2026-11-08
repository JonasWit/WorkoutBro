package com.wtk_apps.workoutbro.ui

import com.wtk_apps.workoutbro.timer.CuePlayer
import com.wtk_apps.workoutbro.timer.MonotonicClock
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import com.wtk_apps.workoutbro.timer.TimerCue
import com.wtk_apps.workoutbro.timer.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The view model is a seam between the UI and the process-scoped controller: it must not
 * add behaviour, only expose the controller and forward commands.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {
  private val scheduler = TestCoroutineScheduler()
  private val controller =
    TimerController(
      cuePlayer =
        object : CuePlayer {
          override fun play(
            cue: TimerCue,
            config: TimerConfig,
          ) = Unit
        },
      clock = MonotonicClock { scheduler.currentTime },
      scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scheduler)),
      tickMillis = 100L,
    )

  private val viewModel = TimerViewModel(controller)

  private fun advance(millis: Long) {
    scheduler.advanceTimeBy(millis)
    scheduler.runCurrent()
  }

  @Test
  fun initialState_isExposedFromTheController() {
    assertEquals(TimerPhase.IDLE, viewModel.state.value.phase)
  }

  @Test
  fun toggle_startsTheSessionAndForwardsTime() {
    viewModel.configure(TimerConfig(rounds = 1, workMillis = 10_000L, restMillis = 0L, prepareMillis = 0L))
    viewModel.toggle()
    advance(1_000L)

    assertTrue(viewModel.state.value.running)
    assertEquals(9_000L, viewModel.state.value.remainingMillis)
  }

  @Test
  fun adjust_isForwarded() {
    viewModel.configure(TimerConfig(rounds = 1, workMillis = 10_000L, restMillis = 0L, prepareMillis = 0L))
    viewModel.toggle()
    viewModel.adjust(5_000L)

    assertEquals(15_000L, viewModel.state.value.remainingMillis)
  }

  @Test
  fun reset_returnsToIdleWithoutStoppingTheClock() {
    viewModel.toggle()
    viewModel.reset()

    assertEquals(TimerPhase.IDLE, viewModel.state.value.phase)
    advance(1_000L)
    assertEquals(TimerPhase.IDLE, viewModel.state.value.phase)
  }
}
