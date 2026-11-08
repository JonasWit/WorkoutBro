package com.wtk_apps.workoutbro.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.wtk_apps.workoutbro.timer.MAX_PHASE_SECONDS
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.wholeSeconds

/**
 * The single mapping between [TimerConfig] and DataStore preferences.
 *
 * The same keys are used for the active configuration (empty prefix) and for every
 * saved preset (`preset_<index>_`), so adding a field means editing one place instead
 * of four.
 */
object ConfigPreferences {
  fun keyRounds(prefix: String) = intPreferencesKey("${prefix}rounds")

  fun keyWorkSeconds(prefix: String) = intPreferencesKey("${prefix}work_seconds")

  fun keyRestSeconds(prefix: String) = intPreferencesKey("${prefix}rest_seconds")

  fun keyPrepareSeconds(prefix: String) = intPreferencesKey("${prefix}prepare_seconds")

  fun keyCountdownSeconds(prefix: String) = intPreferencesKey("${prefix}countdown_seconds")

  fun keyHalfWay(prefix: String) = booleanPreferencesKey("${prefix}half_way")

  fun keyCountdownEnabled(prefix: String) = booleanPreferencesKey("${prefix}countdown_enabled")

  fun keyVibration(prefix: String) = booleanPreferencesKey("${prefix}vibration")

  fun keySound(prefix: String) = booleanPreferencesKey("${prefix}sound")

  /** Reads a configuration, clamping stored values so bad data cannot crash the app. */
  fun read(
    preferences: Preferences,
    prefix: String,
  ): TimerConfig {
    val defaults = TimerConfig()
    return TimerConfig(
      rounds = (preferences[keyRounds(prefix)] ?: defaults.rounds).coerceIn(1, MAX_ROUNDS),
      workMillis =
        (preferences[keyWorkSeconds(prefix)] ?: wholeSeconds(defaults.workMillis))
          .coerceIn(1, MAX_PHASE_SECONDS) * 1_000L,
      restMillis =
        (preferences[keyRestSeconds(prefix)] ?: wholeSeconds(defaults.restMillis))
          .coerceIn(0, MAX_PHASE_SECONDS) * 1_000L,
      prepareMillis =
        (preferences[keyPrepareSeconds(prefix)] ?: wholeSeconds(defaults.prepareMillis))
          .coerceIn(0, MAX_PHASE_SECONDS) * 1_000L,
      countdownSeconds =
        (preferences[keyCountdownSeconds(prefix)] ?: defaults.countdownSeconds)
          .coerceIn(0, MAX_COUNTDOWN_SECONDS),
      halfWayEnabled = preferences[keyHalfWay(prefix)] ?: defaults.halfWayEnabled,
      countdownEnabled = preferences[keyCountdownEnabled(prefix)] ?: defaults.countdownEnabled,
      vibrationEnabled = preferences[keyVibration(prefix)] ?: defaults.vibrationEnabled,
      soundEnabled = preferences[keySound(prefix)] ?: defaults.soundEnabled,
    )
  }

  /** Writes every field of [config] under [prefix], truncating durations to seconds. */
  fun write(
    preferences: MutablePreferences,
    prefix: String,
    config: TimerConfig,
  ) {
    preferences[keyRounds(prefix)] = config.rounds
    preferences[keyWorkSeconds(prefix)] = wholeSeconds(config.workMillis)
    preferences[keyRestSeconds(prefix)] = wholeSeconds(config.restMillis)
    preferences[keyPrepareSeconds(prefix)] = wholeSeconds(config.prepareMillis)
    preferences[keyCountdownSeconds(prefix)] = config.countdownSeconds
    preferences[keyHalfWay(prefix)] = config.halfWayEnabled
    preferences[keyCountdownEnabled(prefix)] = config.countdownEnabled
    preferences[keyVibration(prefix)] = config.vibrationEnabled
    preferences[keySound(prefix)] = config.soundEnabled
  }

  /** Removes every field written under [prefix]. */
  fun clear(
    preferences: MutablePreferences,
    prefix: String,
  ) {
    keys(prefix).forEach { preferences.remove(it) }
  }

  private fun keys(prefix: String) =
    listOf(
      keyRounds(prefix),
      keyWorkSeconds(prefix),
      keyRestSeconds(prefix),
      keyPrepareSeconds(prefix),
      keyCountdownSeconds(prefix),
      keyHalfWay(prefix),
      keyCountdownEnabled(prefix),
      keyVibration(prefix),
      keySound(prefix),
    )

  private const val MAX_ROUNDS = 99
  private const val MAX_COUNTDOWN_SECONDS = 10
}
