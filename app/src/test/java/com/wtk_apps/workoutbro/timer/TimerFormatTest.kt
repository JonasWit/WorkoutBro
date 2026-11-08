package com.wtk_apps.workoutbro.timer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterisation tests for the clock format shown in the dial and the notification.
 * Rounding is deliberate: a running phase must never display 00:00 while time remains.
 */
class TimerFormatTest {
  @Test
  fun wholeSeconds_areShownAsMinutesAndSeconds() {
    assertEquals("00:00", formatMillis(0L))
    assertEquals("00:01", formatMillis(1_000L))
    assertEquals("01:00", formatMillis(60_000L))
    assertEquals("01:30", formatMillis(90_000L))
  }

  @Test
  fun partialSeconds_roundUpSoTheClockDoesNotShowZero() {
    assertEquals("00:01", formatMillis(1L))
    assertEquals("00:01", formatMillis(999L))
    assertEquals("00:02", formatMillis(1_001L))
  }

  @Test
  fun negativeInput_isTreatedAsZero() {
    assertEquals("00:00", formatMillis(-500L))
  }
}
