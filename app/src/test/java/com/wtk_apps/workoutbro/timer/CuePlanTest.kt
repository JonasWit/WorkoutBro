package com.wtk_apps.workoutbro.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapping from a cue to what the device should do is data, not branching code, so
 * it can be tested without Android and so a new cue cannot silently do nothing.
 */
class CuePlanTest {
  @Test
  fun everyCueHasAPlan() {
    TimerCue.entries.forEach { cue ->
      assertTrue("cue $cue has no plan", cuePlan(cue).isNotEmpty())
    }
  }

  @Test
  fun countdownBeepsThenVibratesBriefly() {
    assertEquals(
      listOf(CueStep.Sound(CueSound.BEEP), CueStep.Vibrate(80L)),
      cuePlan(TimerCue.COUNTDOWN),
    )
  }

  @Test
  fun finishedVibratesFirstThenPlaysThreeGongs() {
    assertEquals(
      listOf(
        CueStep.Vibrate(700L),
        CueStep.Sound(CueSound.GONG),
        CueStep.Wait(700L),
        CueStep.Sound(CueSound.GONG),
        CueStep.Wait(700L),
        CueStep.Sound(CueSound.GONG),
      ),
      cuePlan(TimerCue.FINISHED),
    )
  }

  @Test
  fun everyPlanContainsAVibrationAndASound() {
    TimerCue.entries.forEach { cue ->
      val plan = cuePlan(cue)
      assertTrue(
        "cue $cue has no vibration",
        plan.any { it is CueStep.Vibrate && it.millis > 0L },
      )
      assertTrue("cue $cue has no sound", plan.any { it is CueStep.Sound })
    }
  }
}
