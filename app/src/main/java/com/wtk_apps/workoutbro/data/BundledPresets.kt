package com.wtk_apps.workoutbro.data

import android.content.Context

/**
 * The trainings that ship with the app, so a fresh install is usable without typing six
 * configurations. The asset uses the same schema as an exported backup, which means one
 * reader and one set of tests cover both.
 *
 * A malformed asset yields an empty list instead of failing the start-up of an otherwise
 * healthy app; [BundledTrainingsTest] is what stops a broken file from shipping.
 */
object BundledPresets {
  const val ASSET: String = "default_trainings.json"

  fun read(context: Context): List<TrainingPreset> {
    val text = context.assets.open(ASSET).use { it.readBytes().decodeToString() }
    return parse(text)
  }

  fun parse(text: String): List<TrainingPreset> = PresetsBackup.fromJson(text).getOrElse { emptyList() }
}
