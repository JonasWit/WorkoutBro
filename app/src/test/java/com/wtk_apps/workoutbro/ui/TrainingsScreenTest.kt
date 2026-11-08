package com.wtk_apps.workoutbro.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wtk_apps.workoutbro.data.MAX_PRESETS
import com.wtk_apps.workoutbro.data.TrainingPreset
import com.wtk_apps.workoutbro.timer.CuePlayer
import com.wtk_apps.workoutbro.timer.MonotonicClock
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import com.wtk_apps.workoutbro.timer.TimerCue
import com.wtk_apps.workoutbro.ui.trainings.TrainingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * UI checks on the JVM through Robolectric: no emulator, no installation, seconds per run.
 * They cover what unit tests cannot see — that the screen renders the saved trainings and
 * the backup actions.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TrainingsScreenTest {
  @get:Rule val compose = createComposeRule()

  private val timer =
    TimerController(
      cuePlayer =
        object : CuePlayer {
          override fun play(
            cue: TimerCue,
            config: TimerConfig,
          ) = Unit
        },
      clock = MonotonicClock { 0L },
      scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

  @Test
  fun savedTrainingsAreListedWithBackupActions() {
    val viewModel =
      PresetsViewModel(
        FakeWorkoutStore(
          initialPresets = listOf(TrainingPreset("Rano", TimerConfig(rounds = 5))),
        ),
        timer,
      )

    compose.setContent {
      TrainingsScreen(onBack = {}, onEdit = {}, viewModel = viewModel)
    }

    compose.onNodeWithText("Trainings").assertIsDisplayed()
    compose.onNodeWithText("Rano").assertIsDisplayed()
    compose.onNodeWithText("1 / $MAX_PRESETS saved").assertIsDisplayed()
    compose.onNodeWithText("EXPORT").assertIsDisplayed()
    compose.onNodeWithText("IMPORT").assertIsDisplayed()
  }

  @Test
  fun emptyStore_showsTheEmptyState() {
    val viewModel = PresetsViewModel(FakeWorkoutStore(), timer)

    compose.setContent {
      TrainingsScreen(onBack = {}, onEdit = {}, viewModel = viewModel)
    }

    compose
      .onNodeWithText("No saved trainings yet. Configure the timer, then save it here.")
      .assertIsDisplayed()
  }
}
