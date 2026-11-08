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
    TimerCue.PREPARE_START -> listOf(CueStep.Sound(CueSound.BEEP), CueStep.Vibrate(120L))
    TimerCue.WORK_START -> listOf(CueStep.Sound(CueSound.GONG), CueStep.Vibrate(400L))
    TimerCue.WORK_END -> listOf(CueStep.Sound(CueSound.GONG), CueStep.Vibrate(400L))
    TimerCue.HALF_WAY -> listOf(CueStep.Sound(CueSound.BEEP), CueStep.Vibrate(120L))
    TimerCue.COUNTDOWN -> listOf(CueStep.Sound(CueSound.BEEP), CueStep.Vibrate(80L))
    TimerCue.FINISHED ->
      listOf(
        CueStep.Vibrate(700L),
        CueStep.Sound(CueSound.GONG),
        CueStep.Wait(700L),
        CueStep.Sound(CueSound.GONG),
        CueStep.Wait(700L),
        CueStep.Sound(CueSound.GONG),
      )
  }
