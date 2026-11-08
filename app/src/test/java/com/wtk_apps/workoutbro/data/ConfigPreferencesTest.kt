package com.wtk_apps.workoutbro.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.wtk_apps.workoutbro.timer.MAX_PHASE_SECONDS
import com.wtk_apps.workoutbro.timer.TimerConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The mapping between [TimerConfig] and DataStore preferences is one rule, so it must
 * exist once and be usable for both the active configuration and every saved preset.
 */
class ConfigPreferencesTest {
  private val config =
    TimerConfig(
      rounds = 5,
      workMillis = 90_000L,
      restMillis = 45_000L,
      prepareMillis = 10_000L,
      countdownSeconds = 4,
      halfWayEnabled = false,
      countdownEnabled = true,
      vibrationEnabled = false,
      soundEnabled = true,
    )

  @Test
  fun roundTrip_restoresEveryField() {
    val prefs = mutablePreferencesOf()
    ConfigPreferences.write(prefs, prefix = "", config = config)

    assertEquals(config, ConfigPreferences.read(prefs, prefix = ""))
  }

  @Test
  fun differentPrefixes_doNotCollide() {
    val prefs = mutablePreferencesOf()
    val preset = config.copy(rounds = 2, workMillis = 30_000L)
    ConfigPreferences.write(prefs, prefix = "", config = config)
    ConfigPreferences.write(prefs, prefix = "preset_0_", config = preset)

    assertEquals(config, ConfigPreferences.read(prefs, prefix = ""))
    assertEquals(preset, ConfigPreferences.read(prefs, prefix = "preset_0_"))
  }

  @Test
  fun missingPreferences_returnDefaults() {
    val prefs = mutablePreferencesOf()

    assertEquals(TimerConfig(), ConfigPreferences.read(prefs, prefix = ""))
  }

  @Test
  fun outOfRangeValues_areClampedInsteadOfCrashing() {
    val prefs = mutablePreferencesOf()
    ConfigPreferences.write(prefs, prefix = "", config = config)
    prefs[ConfigPreferences.keyRounds("")] = 0
    prefs[ConfigPreferences.keyWorkSeconds("")] = MAX_PHASE_SECONDS + 100

    val restored = ConfigPreferences.read(prefs, prefix = "")

    assertEquals(1, restored.rounds)
    assertEquals(MAX_PHASE_SECONDS * 1_000L, restored.workMillis)
  }

  @Test
  fun subSecondPrecision_isTruncatedOnWrite() {
    val prefs = mutablePreferencesOf()
    ConfigPreferences.write(prefs, prefix = "", config = config.copy(workMillis = 5_500L))

    assertEquals(5_000L, ConfigPreferences.read(prefs, prefix = "").workMillis)
  }
}
