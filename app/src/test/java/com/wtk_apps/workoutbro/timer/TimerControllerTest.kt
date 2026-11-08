package com.wtk_apps.workoutbro.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the controller that drives [TimerEngine] with elapsed time.
 *
 * The clock is a fake tied to the test scheduler, so a virtual second of delay is a
 * virtual second of timer progress. No real time, no real Android, no mocks of the
 * engine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerControllerTest {
  private val scheduler = TestCoroutineScheduler()
  private val dispatcher = StandardTestDispatcher(scheduler)
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private val clock = MonotonicClock { scheduler.currentTime }

  private lateinit var player: RecordingCuePlayer
  private lateinit var controller: TimerController

  @Before
  fun setUp() {
    player = RecordingCuePlayer()
    controller = TimerController(cuePlayer = player, clock = clock, scope = scope, tickMillis = 100L)
    controller.configure(
      TimerConfig(rounds = 3, workMillis = 10_000L, restMillis = 5_000L, prepareMillis = 0L),
    )
  }

  private fun advance(millis: Long) {
    scheduler.advanceTimeBy(millis)
    scheduler.runCurrent()
  }

  @Test
  fun toggle_startsTheSessionAndConsumesElapsedTime() {
    controller.toggle()
    advance(1_000L)

    val state = controller.state.value
    assertEquals(TimerPhase.WORK, state.phase)
    assertTrue(state.running)
    assertEquals(9_000L, state.remainingMillis)
  }

  @Test
  fun pause_stopsConsumingTime() {
    controller.toggle()
    advance(1_000L)
    controller.toggle()
    advance(2_000L)

    val state = controller.state.value
    assertFalse(state.running)
    assertEquals(9_000L, state.remainingMillis)
  }

  @Test
  fun reachingTheEndOfARound_emitsCuesForTheBoundary() {
    controller.toggle()
    advance(10_000L)

    val state = controller.state.value
    assertEquals(TimerPhase.REST, state.phase)
    assertEquals(5_000L, state.remainingMillis)
    assertEquals(1, player.cues.count { it == TimerCue.WORK_END })
  }

  @Test
  fun reset_stopsTheLoopAndReturnsToIdle() {
    controller.toggle()
    advance(1_000L)
    controller.reset()

    assertEquals(TimerPhase.IDLE, controller.state.value.phase)
    advance(5_000L)
    assertEquals(TimerPhase.IDLE, controller.state.value.phase)
  }

  @Test
  fun finishingTheLastRound_stopsRunning() {
    controller.configure(
      TimerConfig(rounds = 1, workMillis = 10_000L, restMillis = 5_000L, prepareMillis = 0L),
    )
    controller.toggle()
    advance(10_000L)

    val state = controller.state.value
    assertEquals(TimerPhase.FINISHED, state.phase)
    assertFalse(state.running)
  }

  @Test
  fun adjust_isAppliedWhileRunning() {
    controller.toggle()
    advance(1_000L)
    controller.adjust(30_000L)

    assertEquals(39_000L, controller.state.value.remainingMillis)
  }

  @Test
  fun configure_whileRunning_keepsTheCurrentSession() {
    controller.toggle()
    advance(1_000L)
    controller.configure(
      TimerConfig(rounds = 3, workMillis = 20_000L, restMillis = 5_000L, prepareMillis = 0L),
    )

    val state = controller.state.value
    assertEquals(TimerPhase.WORK, state.phase)
    assertEquals(9_000L, state.remainingMillis)
    assertEquals(20_000L, state.config.workMillis)
  }

  @Test
  fun configure_whileIdle_resetsToTheNewConfig() {
    controller.configure(
      TimerConfig(rounds = 2, workMillis = 7_000L, restMillis = 3_000L, prepareMillis = 0L),
    )

    val state = controller.state.value
    assertEquals(TimerPhase.IDLE, state.phase)
    assertEquals(7_000L, state.config.workMillis)
  }

  private class RecordingCuePlayer : CuePlayer {
    val cues = mutableListOf<TimerCue>()

    override fun play(
      cue: TimerCue,
      config: TimerConfig,
    ) {
      cues += cue
    }
  }
}
