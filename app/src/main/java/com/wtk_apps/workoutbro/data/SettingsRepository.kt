package com.wtk_apps.workoutbro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wtk_apps.workoutbro.timer.TimerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workoutbro_settings")

/**
 * The app-wide store. The only place that knows where the file lives, so tests can hand
 * [SettingsRepository] a store of their own instead of the one the process shares.
 */
fun settingsRepository(context: Context): SettingsRepository = SettingsRepository(context.applicationContext.dataStore)

/**
 * Persists the active [TimerConfig] and up to [MAX_PRESETS] saved trainings with
 * Preferences DataStore, so both survive an app restart.
 *
 * Field mapping lives in [ConfigPreferences]; this class only handles DataStore access
 * and the preset list.
 */
class SettingsRepository(
  private val dataStore: DataStore<Preferences>,
) : WorkoutStore {
  /** The currently active configuration. */
  override val config: Flow<TimerConfig> = dataStore.data.map { readConfig(it, prefix = "") }

  /** Saved trainings, in the order they were added. */
  override val presets: Flow<List<TrainingPreset>> =
    dataStore.data.map { preferences ->
      val count = preferences.presetCount()
      (0 until count).mapNotNull { index -> readPreset(preferences, index) }
    }

  override suspend fun save(config: TimerConfig) {
    dataStore.edit { ConfigPreferences.write(it, prefix = "", config = config) }
  }

  /**
   * Installs the bundled trainings once: only when this store has never been seeded and
   * holds no trainings yet, so an import or a hand-made training is never pushed out and
   * deleted defaults never come back.
   */
  suspend fun seedDefaults(defaults: List<TrainingPreset>) {
    dataStore.edit { preferences ->
      if (preferences[KEY_DEFAULTS_SEEDED] == true) return@edit
      preferences[KEY_DEFAULTS_SEEDED] = true
      if (preferences.presetCount() > 0) return@edit

      val count = defaults.size.coerceAtMost(MAX_PRESETS)
      defaults.take(count).forEachIndexed { index, preset -> preferences.writePreset(index, preset) }
      preferences[KEY_PRESET_COUNT] = count
    }
  }

  /** Overwrites the saved training at [index]. */
  override suspend fun updatePreset(
    index: Int,
    preset: TrainingPreset,
  ) {
    dataStore.edit { preferences ->
      if (index in 0 until preferences.presetCount()) {
        preferences.writePreset(index, preset)
      }
    }
  }

  /** Stores [preset] if there is room; returns false when the limit is reached. */
  override suspend fun addPreset(preset: TrainingPreset): Boolean {
    var added = false
    dataStore.edit { preferences ->
      val count = preferences.presetCount()
      if (count < MAX_PRESETS) {
        preferences.writePreset(count, preset)
        preferences[KEY_PRESET_COUNT] = count + 1
        added = true
      }
    }
    return added
  }

  override suspend fun deletePreset(index: Int) {
    dataStore.edit { preferences ->
      val count = preferences.presetCount()
      if (index !in 0 until count) return@edit

      for (position in index until count - 1) {
        val next = readPreset(preferences, position + 1)
        if (next == null) preferences.clearPreset(position) else preferences.writePreset(position, next)
      }
      preferences.clearPreset(count - 1)
      preferences[KEY_PRESET_COUNT] = count - 1
    }
  }

  private fun readConfig(
    preferences: Preferences,
    prefix: String,
  ): TimerConfig = ConfigPreferences.read(preferences, prefix)

  private fun readPreset(
    preferences: Preferences,
    index: Int,
  ): TrainingPreset? {
    val name = preferences[nameKey(index)] ?: return null
    return TrainingPreset(name, readConfig(preferences, presetPrefix(index)))
  }

  private fun Preferences.presetCount(): Int = (this[KEY_PRESET_COUNT] ?: 0).coerceIn(0, MAX_PRESETS)

  private fun androidx.datastore.preferences.core.MutablePreferences.writePreset(
    index: Int,
    preset: TrainingPreset,
  ) {
    this[nameKey(index)] = preset.name
    ConfigPreferences.write(this, presetPrefix(index), preset.config)
  }

  private fun androidx.datastore.preferences.core.MutablePreferences.clearPreset(index: Int) {
    remove(nameKey(index))
    ConfigPreferences.clear(this, presetPrefix(index))
  }

  private companion object {
    val KEY_PRESET_COUNT = intPreferencesKey("preset_count")
    val KEY_DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")

    fun nameKey(index: Int) = stringPreferencesKey("preset_${index}_name")

    fun presetPrefix(index: Int) = "preset_${index}_"
  }
}
