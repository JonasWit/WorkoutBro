package com.wtk_apps.workoutbro.timer

/** Formats a duration in milliseconds as `MM:SS`, rounding up to whole seconds. */
fun formatMillis(millis: Long): String {
  val totalSeconds = ((millis + 999L) / 1_000L).coerceAtLeast(0L)
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return "%02d:%02d".format(minutes, seconds)
}

/**
 * Longest allowed phase duration in seconds (99:59). Keeping minutes to two
 * digits means the clock never exceeds the `MM:SS` format.
 */
const val MAX_PHASE_SECONDS = 99 * 60 + 59

/** One contiguous section of a session, used to draw the progress bar. */
data class SessionSegment(
  val phase: TimerPhase,
  val millis: Long,
)

/** Ordered sections of a full session: prepare, then work/rest for each round. */
fun sessionSegments(config: TimerConfig): List<SessionSegment> {
  val out = mutableListOf<SessionSegment>()
  if (config.prepareMillis > 0L) out += SessionSegment(TimerPhase.PREPARE, config.prepareMillis)
  for (round in 1..config.rounds) {
    out += SessionSegment(TimerPhase.WORK, config.workMillis)
    if (round < config.rounds && config.restMillis > 0L) out += SessionSegment(TimerPhase.REST, config.restMillis)
  }
  return out
}

/** Total duration of a full session: prepare + rounds*work + (rounds-1)*rest. */
fun totalSessionMillis(config: TimerConfig): Long {
  val rests = (config.rounds - 1).coerceAtLeast(0)
  return config.prepareMillis + config.rounds * config.workMillis + rests * config.restMillis
}

/** Time left from [state] until the whole session finishes (0 when finished). */
fun totalRemainingMillis(state: TimerState): Long {
  val c = state.config
  val rests = (c.rounds - 1).coerceAtLeast(0)
  return when (state.phase) {
    TimerPhase.IDLE -> totalSessionMillis(c)
    TimerPhase.FINISHED -> 0L
    TimerPhase.PREPARE -> state.remainingMillis + c.rounds * c.workMillis + rests * c.restMillis
    TimerPhase.WORK -> {
      val after = (c.rounds - state.round).coerceAtLeast(0)
      val future =
        if (state.round < c.rounds) {
          c.restMillis + after * c.workMillis + (after - 1).coerceAtLeast(0) * c.restMillis
        } else {
          0L
        }
      state.remainingMillis + future
    }
    TimerPhase.REST -> {
      val after = (c.rounds - state.round).coerceAtLeast(0)
      state.remainingMillis + after * c.workMillis + (after - 1).coerceAtLeast(0) * c.restMillis
    }
  }
}

/** Total duration of a full session: prepare + rounds*work + (rounds-1)*rest. */
