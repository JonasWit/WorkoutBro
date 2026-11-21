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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization tests for [SettingsRepository] preset CRUD and capacity limits.
 */
class SettingsRepositoryTest {
  private val dataStore = InMemoryDataStore()
  private val repository = SettingsRepository(dataStore)

  @Test
  fun addPreset_storesPresetsInOrderUpToMaxLimit() =
    runTest {
      repeat(MAX_PRESETS) { index ->
        assertTrue(repository.addPreset(TrainingPreset("W$index", TimerConfig(rounds = index + 1))))
      }

      assertFalse(repository.addPreset(TrainingPreset("Overflow", TimerConfig())))
      val stored = repository.presets.first()
      assertEquals(MAX_PRESETS, stored.size)
      assertEquals("W0", stored.first().name)
      assertEquals("W${MAX_PRESETS - 1}", stored.last().name)
    }

  @Test
  fun updatePreset_overwritesTargetAndIgnoresOutOfBoundsIndex() =
    runTest {
      repository.addPreset(TrainingPreset("First", TimerConfig(rounds = 2)))
      repository.addPreset(TrainingPreset("Second", TimerConfig(rounds = 3)))

      val updated = TrainingPreset("Edited", TimerConfig(rounds = 7, workMillis = 45_000L))
      repository.updatePreset(1, updated)
      repository.updatePreset(99, TrainingPreset("Ignored", TimerConfig()))

      assertEquals(
        listOf(TrainingPreset("First", TimerConfig(rounds = 2)), updated),
        repository.presets.first(),
      )
    }

  @Test
  fun deletePreset_shiftsRemainingPresetsAndClearsTrailingSlot() =
    runTest {
      val a = TrainingPreset("A", TimerConfig(rounds = 1))
      val b = TrainingPreset("B", TimerConfig(rounds = 2))
      val c = TrainingPreset("C", TimerConfig(rounds = 3))
      repository.addPreset(a)
      repository.addPreset(b)
      repository.addPreset(c)

      repository.deletePreset(1)
      repository.deletePreset(-1)
      repository.deletePreset(5)

      assertEquals(listOf(a, c), repository.presets.first())
      // Adding another preset must overwrite the cleared trailing slot cleanly.
      val d = TrainingPreset("D", TimerConfig(rounds = 4))
      assertTrue(repository.addPreset(d))
      assertEquals(listOf(a, c, d), repository.presets.first())
    }

  private class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
      val updated = transform(state.value)
      state.value = updated
      return updated
    }
  }
}
