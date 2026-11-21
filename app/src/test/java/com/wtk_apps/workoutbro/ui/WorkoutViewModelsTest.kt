package com.wtk_apps.workoutbro.ui

import com.wtk_apps.workoutbro.data.TrainingPreset
import com.wtk_apps.workoutbro.timer.CuePlayer
import com.wtk_apps.workoutbro.timer.MonotonicClock
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import com.wtk_apps.workoutbro.timer.TimerCue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * View models are seams: they expose ports to Compose and forward commands. These tests use
 * an in-memory store and a controller with a fake clock, so no Android and no DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelsTest {
  private val scheduler = TestCoroutineScheduler()
  private val dispatcher = StandardTestDispatcher(scheduler)
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
      scope = CoroutineScope(SupervisorJob() + dispatcher),
    )

  @Before
  fun setUp() = Dispatchers.setMain(dispatcher)

  @After
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun settingsViewModel_exposesTheStoredConfig() =
    runTest(dispatcher) {
      val store = FakeWorkoutStore(initialConfig = TimerConfig(rounds = 4, workMillis = 20_000L))
      val viewModel = SettingsViewModel(store, timer)
      advanceUntilIdle()

      assertEquals(4, viewModel.config.value.rounds)
    }

  @Test
  fun settingsViewModel_savePersistsAndAppliesToTheTimer() =
    runTest(dispatcher) {
      val store = FakeWorkoutStore()
      val viewModel = SettingsViewModel(store, timer)
      val newConfig = TimerConfig(rounds = 2, workMillis = 30_000L)

      viewModel.save(newConfig, onSaved = {})
      advanceUntilIdle()

      assertEquals(newConfig, store.savedConfig)
      assertEquals(30_000L, timer.state.value.config.workMillis)
    }

  @Test
  fun presetsViewModel_savesTheActiveConfiguration() =
    runTest(dispatcher) {
      val store = FakeWorkoutStore()
      timer.configure(TimerConfig(rounds = 5, workMillis = 45_000L))
      val viewModel = PresetsViewModel(store, timer)
      var added = false

      viewModel.saveCurrent("Rano", onResult = { added = it })
      advanceUntilIdle()

      assertTrue(added)
      assertEquals(1, store.presetsState.value.size)
      assertEquals(
        45_000L,
        store.presetsState.value
          .single()
          .config.workMillis,
      )
    }

  @Test
  fun presetsViewModel_applySavesAndConfiguresTheTimer() =
    runTest(dispatcher) {
      val store = FakeWorkoutStore()
      val viewModel = PresetsViewModel(store, timer)
      val preset = TrainingPreset("Wieczór", TimerConfig(rounds = 1, workMillis = 60_000L))

      viewModel.apply(preset, onApplied = {})
      advanceUntilIdle()

      assertEquals(preset.config, store.savedConfig)
      assertEquals(60_000L, timer.state.value.config.workMillis)
    }

  @Test
  fun presetsViewModel_importStopsWhenTheStoreIsFull() =
    runTest(dispatcher) {
      val store = FakeWorkoutStore(capacity = 1)
      val viewModel = PresetsViewModel(store, timer)
      var result: ImportResult? = null

      viewModel.importPresets(
        listOf(TrainingPreset("A", TimerConfig()), TrainingPreset("B", TimerConfig())),
      ) { result = it }
      advanceUntilIdle()

      assertEquals(1, result?.added)
      assertEquals(1, result?.skipped)
      assertEquals(1, store.presetsState.value.size)
    }

  @Test
  fun presetsViewModel_deleteForwardsTheIndex() =
    runTest(dispatcher) {
      val store = FakeWorkoutStore()
      val viewModel = PresetsViewModel(store, timer)

      viewModel.delete(2)
      advanceUntilIdle()

      assertEquals(2, store.deletedIndex)
    }
}
