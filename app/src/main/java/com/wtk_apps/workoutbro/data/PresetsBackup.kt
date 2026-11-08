package com.wtk_apps.workoutbro.data

import com.wtk_apps.workoutbro.timer.MAX_PHASE_SECONDS
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.wholeSeconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Portable backup of saved trainings.
 *
 * The file carries a schema version so a future format can be detected instead of
 * misread. Unknown fields are ignored, which keeps older builds able to read files
 * written by newer ones. Invalid JSON is a failure, never an empty list: silently
 * "restoring" nothing would look like success while losing everything.
 */
object PresetsBackup {
  const val SCHEMA: Int = 1

  private val json =
    Json {
      ignoreUnknownKeys = true
      prettyPrint = true
      encodeDefaults = true
    }

  fun toJson(presets: List<TrainingPreset>): String =
    json.encodeToString(
      BackupFile(schema = SCHEMA, presets = presets.map { it.toDto() }),
    )

  fun fromJson(text: String): Result<List<TrainingPreset>> =
    runCatching {
      val file = json.decodeFromString<BackupFile>(text)
      require(file.schema == SCHEMA) { "unsupported schema: ${file.schema}" }
      file.presets.map { it.toPreset() }
    }

  @Serializable
  private data class BackupFile(
    val schema: Int,
    val presets: List<PresetDto>,
  )

  @Serializable
  private data class PresetDto(
    val name: String,
    val rounds: Int,
    val workSeconds: Int,
    val restSeconds: Int,
    val prepareSeconds: Int,
    val countdownSeconds: Int,
    val halfWay: Boolean,
    val countdown: Boolean,
    val vibration: Boolean,
    val sound: Boolean,
  )

  private fun TrainingPreset.toDto(): PresetDto =
    PresetDto(
      name = name,
      rounds = config.rounds,
      workSeconds = wholeSeconds(config.workMillis),
      restSeconds = wholeSeconds(config.restMillis),
      prepareSeconds = wholeSeconds(config.prepareMillis),
      countdownSeconds = config.countdownSeconds,
      halfWay = config.halfWayEnabled,
      countdown = config.countdownEnabled,
      vibration = config.vibrationEnabled,
      sound = config.soundEnabled,
    )

  private fun PresetDto.toPreset(): TrainingPreset =
    TrainingPreset(
      name = name,
      config =
        TimerConfig(
          rounds = rounds.coerceIn(1, MAX_ROUNDS),
          workMillis = workSeconds.coerceIn(1, MAX_PHASE_SECONDS) * 1_000L,
          restMillis = restSeconds.coerceIn(0, MAX_PHASE_SECONDS) * 1_000L,
          prepareMillis = prepareSeconds.coerceIn(0, MAX_PHASE_SECONDS) * 1_000L,
          countdownSeconds = countdownSeconds.coerceIn(0, MAX_COUNTDOWN_SECONDS),
          halfWayEnabled = halfWay,
          countdownEnabled = countdown,
          vibrationEnabled = vibration,
          soundEnabled = sound,
        ),
    )

  private const val MAX_ROUNDS = 99
  private const val MAX_COUNTDOWN_SECONDS = 10
}
