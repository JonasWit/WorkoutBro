package com.wtk_apps.workoutbro.timer

/**
 * Monotonic millisecond clock, injected so the controller can be driven by a fake clock
 * in tests instead of reading wall time itself.
 */
fun interface MonotonicClock {
  fun nowMillis(): Long
}
