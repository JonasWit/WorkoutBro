package com.wtk_apps.workoutbro.timer

/** Sound assets the app ships. */
enum class CueSound { BEEP, GONG }

/** One step in the reaction to a cue. */
sealed interface CueStep {
  data class Sound(
    val sound: CueSound,
  ) : CueStep

  data class Vibrate(
    val millis: Long,
  ) : CueStep

  data class Wait(
    val millis: Long,
  ) : CueStep
}

/**
 * What the device should do for a given cue, in order.
 *
 * This is a table rather than a `when` in the adapter: the sequence is data, so it is
 * testable, and adding a [TimerCue] shows up in the exhaustiveness test instead of
 * silently doing nothing.
 */
fun cuePlan(cue: TimerCue): List<CueStep> =
  when (cue) {
    TimerCue.PREPARE_START -> listOf(CueStep.Sound(CueSound.BEEP), CueStep.Vibrate(MEDIUM_VIBRATE_MS))
    TimerCue.WORK_START -> listOf(CueStep.Sound(CueSound.GONG), CueStep.Vibrate(LONG_VIBRATE_MS))
    TimerCue.WORK_END -> listOf(CueStep.Sound(CueSound.GONG), CueStep.Vibrate(LONG_VIBRATE_MS))
    TimerCue.HALF_WAY -> listOf(CueStep.Sound(CueSound.BEEP), CueStep.Vibrate(MEDIUM_VIBRATE_MS))
    TimerCue.COUNTDOWN -> listOf(CueStep.Sound(CueSound.BEEP), CueStep.Vibrate(SHORT_VIBRATE_MS))
    TimerCue.FINISHED ->
      listOf(
        CueStep.Vibrate(FINISH_VIBRATE_MS),
        CueStep.Sound(CueSound.GONG),
        CueStep.Wait(FINISH_GONG_GAP_MS),
        CueStep.Sound(CueSound.GONG),
        CueStep.Wait(FINISH_GONG_GAP_MS),
        CueStep.Sound(CueSound.GONG),
      )
  }

private const val SHORT_VIBRATE_MS = 80L
private const val MEDIUM_VIBRATE_MS = 120L
private const val LONG_VIBRATE_MS = 400L
private const val FINISH_VIBRATE_MS = 700L
private const val FINISH_GONG_GAP_MS = 700L
