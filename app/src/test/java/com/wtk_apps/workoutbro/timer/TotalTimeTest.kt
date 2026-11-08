package com.wtk_apps.workoutbro.timer

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the total-session and total-remaining time helpers. */
class TotalTimeTest {
  private fun config(
    rounds: Int = 3,
    workMillis: Long = 10_000L,
    restMillis: Long = 5_000L,
    prepareMillis: Long = 5_000L,
  ) = TimerConfig(
    rounds = rounds,
    workMillis = workMillis,
    restMillis = restMillis,
    prepareMillis = prepareMillis,
  )

  @Test
  fun totalSession_countsPrepareWorkAndRestsBetweenRounds() {
    // 5 + 3*10 + 2*5 = 45 s
    assertEquals(45_000L, totalSessionMillis(config()))
  }

  @Test
  fun idle_remainingEqualsTotalSession() {
    val state = TimerEngine.initialState(config())
    assertEquals(45_000L, totalRemainingMillis(state))
  }

  @Test
  fun prepare_remainingCountsPrepareLeftPlusAllRounds() {
    val started = TimerEngine.start(TimerEngine.initialState(config())).state
    val twoSecondsIn = TimerEngine.tick(started, 2_000L).state
    // prepare left 3s + 3*10 + 2*5 = 43 s
    assertEquals(43_000L, totalRemainingMillis(twoSecondsIn))
  }

  @Test
  fun work_remainingIncludesRestAndLaterRounds() {
    val started = TimerEngine.start(TimerEngine.initialState(config(prepareMillis = 0L))).state
    val fourSecondsIn = TimerEngine.tick(started, 4_000L).state
    // 6s left in round 1 + rest 5 + round2 10 + rest 5 + round3 10 = 36 s
    assertEquals(36_000L, totalRemainingMillis(fourSecondsIn))
  }

  @Test
  fun lastWorkRound_hasNoRestAfterIt() {
    val started = TimerEngine.start(TimerEngine.initialState(config(rounds = 1, prepareMillis = 0L))).state
    val eightSecondsIn = TimerEngine.tick(started, 8_000L).state
    // only 2s left, no rest afterwards
    assertEquals(2_000L, totalRemainingMillis(eightSecondsIn))
  }

  @Test
  fun finished_hasZeroRemaining() {
    val started = TimerEngine.start(TimerEngine.initialState(config(rounds = 1, prepareMillis = 0L))).state
    val finished = TimerEngine.tick(started, 10_000L).state
    assertEquals(TimerPhase.FINISHED, finished.phase)
    assertEquals(0L, totalRemainingMillis(finished))
  }

  @Test
  fun walkingThroughTheSession_consumesExactlyTheTotalTime() {
    val cfg = config(rounds = 4, workMillis = 3_000L, restMillis = 2_000L, prepareMillis = 1_000L)
    var state = TimerEngine.start(TimerEngine.initialState(cfg)).state
    var elapsed = 0L
    var guard = 0
    while (state.phase != TimerPhase.FINISHED && guard < 10_000) {
      state = TimerEngine.tick(state, 1_000L).state
      elapsed += 1_000L
      guard++
    }
    assertEquals(TimerPhase.FINISHED, state.phase)
    // 1 + 4*3 + 3*2 = 19 s
    assertEquals(totalSessionMillis(cfg), elapsed)
    assertEquals(0L, totalRemainingMillis(state))
  }

  @Test
  fun remaining_decreasesExactlyByElapsedTime() {
    val cfg = config(rounds = 3, workMillis = 5_000L, restMillis = 4_000L, prepareMillis = 3_000L)
    var state = TimerEngine.start(TimerEngine.initialState(cfg)).state
    while (state.phase != TimerPhase.FINISHED) {
      val before = totalRemainingMillis(state)
      state = TimerEngine.tick(state, 500L).state
      val after = totalRemainingMillis(state)
      if (state.phase != TimerPhase.FINISHED) {
        assertEquals("remaining should drop by the tick", before - 500L, after)
      }
    }
    assertEquals(0L, totalRemainingMillis(state))
  }

  @Test
  fun sessionSegments_listsPrepareThenWorkAndRestPerRound() {
    val cfg = config(rounds = 3, workMillis = 10_000L, restMillis = 5_000L, prepareMillis = 5_000L)
    val segments = sessionSegments(cfg)
    assertEquals(
      listOf(
        TimerPhase.PREPARE to 5_000L,
        TimerPhase.WORK to 10_000L,
        TimerPhase.REST to 5_000L,
        TimerPhase.WORK to 10_000L,
        TimerPhase.REST to 5_000L,
        TimerPhase.WORK to 10_000L,
      ),
      segments.map { it.phase to it.millis },
    )
    // The sections add up to the whole session.
    assertEquals(totalSessionMillis(cfg), segments.sumOf { it.millis })
  }

  @Test
  fun sessionSegments_omitsPrepareAndTrailingRestWhenAbsent() {
    val cfg = config(rounds = 1, workMillis = 4_000L, restMillis = 5_000L, prepareMillis = 0L)
    assertEquals(listOf(TimerPhase.WORK to 4_000L), sessionSegments(cfg).map { it.phase to it.millis })
  }
}
