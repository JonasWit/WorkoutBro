package com.wtk_apps.workoutbro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.wtk_apps.workoutbro.timer.TimerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bundled trainings must appear exactly once, only on a store that starts empty, and
 * never come back after the user deletes them.
 */
class PresetDefaultsTest {
  private val dataStore = InMemoryPreferences()

  private val defaults =
    listOf(
      TrainingPreset("Rano", TimerConfig(rounds = 2, workMillis = 30_000L)),
      TrainingPreset("Wieczór", TimerConfig(rounds = 3, workMillis = 60_000L)),
    )

  @Test
  fun firstStartInstallsTheDefaults() =
    runTest {
      val store = SettingsRepository(dataStore)

      store.seedDefaults(defaults)

      assertEquals(defaults, store.presets.first())
    }

  @Test
  fun seedingTwiceKeepsOneCopyOfEachDefault() =
    runTest {
      val store = SettingsRepository(dataStore)
      store.seedDefaults(defaults)

      store.seedDefaults(defaults)

      assertEquals(defaults, store.presets.first())
    }

  @Test
  fun storeThatAlreadyHasTrainingsKeepsThemAndSkipsTheDefaults() =
    runTest {
      val store = SettingsRepository(dataStore)
      val own = TrainingPreset("Moje", TimerConfig(rounds = 4))
      assertTrue(store.addPreset(own))

      store.seedDefaults(defaults)

      assertEquals(listOf(own), store.presets.first())
    }

  @Test
  fun deletingEveryTrainingDoesNotBringTheDefaultsBack() =
    runTest {
      val store = SettingsRepository(dataStore)
      store.seedDefaults(defaults)
      repeat(defaults.size) { store.deletePreset(0) }

      store.seedDefaults(defaults)

      assertTrue(store.presets.first().isEmpty())
    }

  /**
   * A file-backed DataStore cannot be used here: on Windows renaming the temporary file
   * over the live one fails whenever a read overlaps a write, which made these tests
   * intermittent. The real file round-trip is covered by AppIdentityTest; these tests own
   * the seeding rules.
   */
  private class InMemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
      val updated = transform(state.value)
      state.value = updated
      return updated
    }
  }
}
