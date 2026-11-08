package com.wtk_apps.workoutbro.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure [TimerEngine] state machine.
 *
 * These tests express the required behaviour of the interval timer: phases
 * (prepare -> work -> rest -> ... -> finished), pause/resume, skip, on-the-fly
 * adjustment and audio/vibration cue emission.
 */
class TimerEngineTest {
  private fun config(
    rounds: Int = 3,
    workMillis: Long = 10_000L,
    restMillis: Long = 5_000L,
    prepareMillis: Long = 0L,
    halfWayEnabled: Boolean = true,
    countdownEnabled: Boolean = true,
  ) = TimerConfig(
    rounds = rounds,
    workMillis = workMillis,
    restMillis = restMillis,
    prepareMillis = prepareMillis,
    halfWayEnabled = halfWayEnabled,
    countdownEnabled = countdownEnabled,
    countdownSeconds = 3,
    vibrationEnabled = true,
  )

  @Test
  fun initialState_isIdleAndNotRunning() {
    val state = TimerEngine.initialState(config())
    assertEquals(TimerPhase.IDLE, state.phase)
    assertFalse(state.running)
    assertEquals(0, state.round)
  }

  @Test
  fun start_withPrepare_entersPrepareWithCue() {
    val transition = TimerEngine.start(TimerEngine.initialState(config(prepareMillis = 10_000L)))
    assertEquals(TimerPhase.PREPARE, transition.state.phase)
    assertEquals(10_000L, transition.state.remainingMillis)
    assertTrue(transition.state.running)
    assertTrue(transition.cues.contains(TimerCue.PREPARE_START))
  }

  @Test
  fun start_withoutPrepare_entersFirstRoundWithWorkStartCue() {
    val transition = TimerEngine.start(TimerEngine.initialState(config(prepareMillis = 0L)))
    assertEquals(TimerPhase.WORK, transition.state.phase)
    assertEquals(1, transition.state.round)
    assertEquals(10_000L, transition.state.remainingMillis)
    assertTrue(transition.cues.contains(TimerCue.WORK_START))
  }

  @Test
  fun tick_throughPrepare_movesToFirstWorkRound() {
    val started = TimerEngine.start(TimerEngine.initialState(config(prepareMillis = 3_000L))).state
    val transition = TimerEngine.tick(started, 3_000L)
    assertEquals(TimerPhase.WORK, transition.state.phase)
    assertEquals(1, transition.state.round)
    assertEquals(10_000L, transition.state.remainingMillis)
    assertTrue(transition.cues.contains(TimerCue.WORK_START))
  }

  @Test
  fun tick_throughWorkRound_movesToRestAfterWorkEnd() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val transition = TimerEngine.tick(started, 10_000L)
    assertEquals(TimerPhase.REST, transition.state.phase)
    assertEquals(1, transition.state.round)
    assertEquals(5_000L, transition.state.remainingMillis)
    assertTrue(transition.cues.contains(TimerCue.WORK_END))
  }

  @Test
  fun tick_throughRest_movesToNextRoundWithWorkStartCue() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val inRest = TimerEngine.tick(started, 10_000L).state
    val transition = TimerEngine.tick(inRest, 5_000L)
    assertEquals(TimerPhase.WORK, transition.state.phase)
    assertEquals(2, transition.state.round)
    assertEquals(10_000L, transition.state.remainingMillis)
    assertTrue(transition.cues.contains(TimerCue.WORK_START))
  }

  @Test
  fun lastWorkRound_finishesWithFinishedCueAndStops() {
    val started = TimerEngine.start(TimerEngine.initialState(config(rounds = 1))).state
    val transition = TimerEngine.tick(started, 10_000L)
    assertEquals(TimerPhase.FINISHED, transition.state.phase)
    assertFalse(transition.state.running)
    assertTrue(transition.cues.contains(TimerCue.WORK_END))
    assertTrue(transition.cues.contains(TimerCue.FINISHED))
  }

  @Test
  fun workRound_emitsHalfWayCueAtHalfTime() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val transition = TimerEngine.tick(started, 5_000L)
    assertEquals(5_000L, transition.state.remainingMillis)
    assertTrue(transition.cues.contains(TimerCue.HALF_WAY))
  }

  @Test
  fun workRound_emitsSingleCountdownCueInLastThreeSeconds() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val atThree = TimerEngine.tick(started, 7_000L).state
    assertEquals(3_000L, atThree.remainingMillis)
    val transition = TimerEngine.tick(atThree, 1_000L)
    assertEquals(2_000L, transition.state.remainingMillis)
    assertEquals(1, transition.cues.count { it == TimerCue.COUNTDOWN })
  }

  @Test
  fun paused_tickDoesNotAdvance() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val paused = TimerEngine.pause(started)
    val transition = TimerEngine.tick(paused, 5_000L)
    assertEquals(paused, transition.state)
    assertTrue(transition.cues.isEmpty())
  }

  @Test
  fun toggle_resumesWhenPaused() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val paused = TimerEngine.pause(started)
    val resumed = TimerEngine.toggle(paused)
    assertTrue(resumed.state.running)
    assertEquals(paused.remainingMillis, resumed.state.remainingMillis)
  }

  @Test
  fun skip_advancesToNextPhaseImmediately() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val transition = TimerEngine.skip(started)
    assertEquals(TimerPhase.REST, transition.state.phase)
    assertTrue(transition.cues.contains(TimerCue.WORK_END))
  }

  @Test
  fun adjustRemaining_changesRemainingWithinBounds() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val longer = TimerEngine.adjustRemaining(started, 30_000L)
    assertEquals(40_000L, longer.remainingMillis)
    val shorter = TimerEngine.adjustRemaining(longer, -100_000L)
    assertEquals(1_000L, shorter.remainingMillis)
  }

  @Test
  fun progress_reportsElapsedFraction() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val quarter = TimerEngine.tick(started, 2_500L).state
    assertEquals(0.25f, quarter.progress, 0.001f)
  }

  @Test
  fun reset_returnsToIdle() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val running = TimerEngine.tick(started, 2_000L).state
    val reset = TimerEngine.reset(running.config)
    assertEquals(TimerPhase.IDLE, reset.phase)
    assertFalse(reset.running)
  }

  @Test
  fun rest_emitsThreeCountdownCuesBeforeNextRound() {
    val started = TimerEngine.start(TimerEngine.initialState(config(restMillis = 5_000L))).state
    val inRest = TimerEngine.tick(started, 10_000L).state
    assertEquals(TimerPhase.REST, inRest.phase)

    val atThree = TimerEngine.tick(inRest, 2_000L)
    val atTwo = TimerEngine.tick(atThree.state, 1_000L)
    val atOne = TimerEngine.tick(atTwo.state, 1_000L)
    val intoRound = TimerEngine.tick(atOne.state, 1_000L)

    assertEquals(1, atThree.cues.count { it == TimerCue.COUNTDOWN })
    assertEquals(1, atTwo.cues.count { it == TimerCue.COUNTDOWN })
    assertEquals(1, atOne.cues.count { it == TimerCue.COUNTDOWN })
    assertEquals(TimerPhase.WORK, intoRound.state.phase)
    assertTrue(intoRound.cues.contains(TimerCue.WORK_START))
  }

  @Test
  fun prepare_emitsCountdownBeforeFirstRound() {
    val started = TimerEngine.start(TimerEngine.initialState(config(prepareMillis = 5_000L))).state
    assertEquals(TimerPhase.PREPARE, started.phase)

    val atThree = TimerEngine.tick(started, 2_000L)
    val atTwo = TimerEngine.tick(atThree.state, 1_000L)
    val atOne = TimerEngine.tick(atTwo.state, 1_000L)
    val intoRound = TimerEngine.tick(atOne.state, 1_000L)

    assertEquals(1, atThree.cues.count { it == TimerCue.COUNTDOWN })
    assertEquals(1, atTwo.cues.count { it == TimerCue.COUNTDOWN })
    assertEquals(1, atOne.cues.count { it == TimerCue.COUNTDOWN })
    assertEquals(TimerPhase.WORK, intoRound.state.phase)
    assertTrue(intoRound.cues.contains(TimerCue.WORK_START))
  }

  @Test
  fun rest_doesNotEmitHalfWayCue() {
    val started = TimerEngine.start(TimerEngine.initialState(config(restMillis = 6_000L))).state
    val inRest = TimerEngine.tick(started, 10_000L).state
    val atHalf = TimerEngine.tick(inRest, 3_000L)
    assertFalse(atHalf.cues.contains(TimerCue.HALF_WAY))
  }
}
