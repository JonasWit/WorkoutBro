package com.wtk_apps.workoutbro.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.wtk_apps.workoutbro.timer.MAX_COUNTDOWN_SECONDS
import com.wtk_apps.workoutbro.timer.MAX_PHASE_SECONDS
import com.wtk_apps.workoutbro.timer.MAX_ROUNDS
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

  /** Reads a configuration, clamping stored values so bad data cannot crash the app. */
  fun read(
    preferences: Preferences,
    prefix: String,
  ): TimerConfig {
    val defaults = TimerConfig()
    val keys = PrefKeys(prefix)
    return TimerConfig(
      rounds = (preferences[keys.rounds] ?: defaults.rounds).coerceIn(1, MAX_ROUNDS),
      workMillis =
        (preferences[keys.workSeconds] ?: wholeSeconds(defaults.workMillis))
          .coerceIn(1, MAX_PHASE_SECONDS) * 1_000L,
      restMillis =
        (preferences[keys.restSeconds] ?: wholeSeconds(defaults.restMillis))
          .coerceIn(0, MAX_PHASE_SECONDS) * 1_000L,
      prepareMillis =
        (preferences[keys.prepareSeconds] ?: wholeSeconds(defaults.prepareMillis))
          .coerceIn(0, MAX_PHASE_SECONDS) * 1_000L,
      countdownSeconds =
        (preferences[keys.countdownSeconds] ?: defaults.countdownSeconds)
          .coerceIn(0, MAX_COUNTDOWN_SECONDS),
      halfWayEnabled = preferences[keys.halfWay] ?: defaults.halfWayEnabled,
      countdownEnabled = preferences[keys.countdownEnabled] ?: defaults.countdownEnabled,
      vibrationEnabled = preferences[keys.vibration] ?: defaults.vibrationEnabled,
      soundEnabled = preferences[keys.sound] ?: defaults.soundEnabled,
    )
  }

  /** Writes every field of [config] under [prefix], truncating durations to seconds. */
  fun write(
    preferences: MutablePreferences,
    prefix: String,
    config: TimerConfig,
  ) {
    val keys = PrefKeys(prefix)
    preferences[keys.rounds] = config.rounds
    preferences[keys.workSeconds] = wholeSeconds(config.workMillis)
    preferences[keys.restSeconds] = wholeSeconds(config.restMillis)
    preferences[keys.prepareSeconds] = wholeSeconds(config.prepareMillis)
    preferences[keys.countdownSeconds] = config.countdownSeconds
    preferences[keys.halfWay] = config.halfWayEnabled
    preferences[keys.countdownEnabled] = config.countdownEnabled
    preferences[keys.vibration] = config.vibrationEnabled
    preferences[keys.sound] = config.soundEnabled
  }

  /** Removes every field written under [prefix]. */
  fun clear(
    preferences: MutablePreferences,
    prefix: String,
  ) {
    PrefKeys(prefix).all.forEach { preferences.remove(it) }
  }

  private class PrefKeys(
    prefix: String,
  ) {
    val rounds = keyRounds(prefix)
    val workSeconds = keyWorkSeconds(prefix)
    val restSeconds = intPreferencesKey("${prefix}rest_seconds")
    val prepareSeconds = intPreferencesKey("${prefix}prepare_seconds")
    val countdownSeconds = intPreferencesKey("${prefix}countdown_seconds")
    val halfWay = booleanPreferencesKey("${prefix}half_way")
    val countdownEnabled = booleanPreferencesKey("${prefix}countdown_enabled")
    val vibration = booleanPreferencesKey("${prefix}vibration")
    val sound = booleanPreferencesKey("${prefix}sound")

    val all =
      listOf(
        rounds,
        workSeconds,
        restSeconds,
        prepareSeconds,
        countdownSeconds,
        halfWay,
        countdownEnabled,
        vibration,
        sound,
      )
  }
}
