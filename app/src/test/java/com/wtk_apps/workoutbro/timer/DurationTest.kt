package com.wtk_apps.workoutbro.timer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * One conversion between milliseconds and minutes/seconds, used by persistence and the
 * configuration form. Truncation is deliberate: stored durations are whole seconds.
 */
class DurationTest {
  @Test
  fun wholeSeconds_truncatesTowardsZero() {
    assertEquals(5, wholeSeconds(5_999L))
    assertEquals(90, wholeSeconds(90_000L))
    assertEquals(0, wholeSeconds(0L))
  }

  @Test
  fun durationParts_splitsMinutesAndSeconds() {
    assertEquals(DurationParts(minutes = 1, seconds = 30), durationParts(90_000L))
    assertEquals(DurationParts(minutes = 0, seconds = 0), durationParts(0L))
    assertEquals(DurationParts(minutes = 99, seconds = 59), durationParts(5_999_000L))
  }

  @Test
  fun millisFromParts_rebuildsTheDuration() {
    assertEquals(90_000L, millisFromParts(minutes = 1, seconds = 30))
    assertEquals(0L, millisFromParts(minutes = 0, seconds = 0))
  }

  @Test
  fun roundTrip_overEveryWholeSecondInRange() {
    for (seconds in 0..MAX_PHASE_SECONDS) {
      val millis = seconds * 1_000L
      val parts = durationParts(millis)
      assertEquals(millis, millisFromParts(parts.minutes, parts.seconds))
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun durationParts_rejectsNegativeInput() {
    durationParts(-1L)
  }
}
