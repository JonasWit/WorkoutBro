package com.wtk_apps.workoutbro.data

import com.wtk_apps.workoutbro.timer.TimerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Export/import is the only protection against losing saved trainings, so the format must
 * round-trip exactly and must fail loudly on garbage instead of silently producing an
 * empty list.
 */
class PresetsBackupTest {
  private val presets =
    listOf(
      TrainingPreset(
        name = "Rano",
        config =
          TimerConfig(
            rounds = 5,
            workMillis = 45_000L,
            restMillis = 15_000L,
            prepareMillis = 10_000L,
            countdownSeconds = 4,
            halfWayEnabled = false,
            countdownEnabled = true,
            vibrationEnabled = false,
            soundEnabled = true,
          ),
      ),
      TrainingPreset(name = "Wieczór", config = TimerConfig(rounds = 1, workMillis = 60_000L)),
    )

  @Test
  fun roundTrip_restoresEveryPresetAndField() {
    val restored = PresetsBackup.fromJson(PresetsBackup.toJson(presets)).getOrNull()

    assertEquals(presets, restored)
  }

  @Test
  fun jsonCarriesTheSchemaVersion() {
    val json = PresetsBackup.toJson(presets)

    assertTrue("schema version missing", json.contains("\"schema\""))
  }

  @Test
  fun malformedJson_returnsFailure() {
    val result = PresetsBackup.fromJson("""{"schema":1,"presets":[{"name":}]}""")

    assertTrue("expected failure, got $result", result.isFailure)
  }

  @Test
  fun unknownFields_areIgnored() {
    val json =
      """
      {"schema":1,"future":"ignored","presets":[
        {"name":"Nowy","rounds":2,"workSeconds":30,"restSeconds":10,"prepareSeconds":0,
         "countdownSeconds":3,"halfWay":true,"countdown":true,"vibration":true,"sound":true,
         "extra":"ignored"}]}
      """

    val restored = PresetsBackup.fromJson(json).getOrNull()

    assertEquals(1, restored?.size)
    assertEquals("Nowy", restored?.single()?.name)
  }

  /**
   * The file WorkoutBase wrote carries no app name, only the schema, so a rename must never
   * stop the old export from importing. This fixture is what v1.0.5 produced.
   */
  @Test
  fun backupFileFromWorkoutBase_importsIntoWorkoutBro() {
    val legacy =
      """
      {
          "schema": 1,
          "presets": [
              {
                  "name": "Rano",
                  "rounds": 5,
                  "workSeconds": 45,
                  "restSeconds": 15,
                  "prepareSeconds": 10,
                  "countdownSeconds": 4,
                  "halfWay": false,
                  "countdown": true,
                  "vibration": false,
                  "sound": true
              }
          ]
      }
      """

    val restored = PresetsBackup.fromJson(legacy).getOrNull()

    assertEquals(presets.first(), restored?.single())
  }

  @Test
  fun outOfRangeValues_areClampedInsteadOfRejected() {
    val json =
      """
      {"schema":1,"presets":[
        {"name":"Dziwny","rounds":0,"workSeconds":0,"restSeconds":-5,"prepareSeconds":0,
         "countdownSeconds":99,"halfWay":true,"countdown":true,"vibration":true,"sound":true}]}
      """

    val config =
      PresetsBackup
        .fromJson(json)
        .getOrNull()
        ?.single()
        ?.config

    assertEquals(1, config?.rounds)
    assertEquals(1_000L, config?.workMillis)
    assertEquals(0L, config?.restMillis)
    assertEquals(10, config?.countdownSeconds)
  }
}
