package com.wtk_apps.workoutbro.timer

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_MINUTE_LONG = 60L
private const val MAX_SECOND_PART = 59

/** A duration split into the parts a person edits: whole minutes and seconds 0..59. */
data class DurationParts(
  val minutes: Int,
  val seconds: Int,
) {
  init {
    require(minutes >= 0) { "minutes must not be negative" }
    require(seconds in 0..MAX_SECOND_PART) { "seconds must be in 0..59" }
  }

  val totalSeconds: Int
    get() = minutes * SECONDS_PER_MINUTE + seconds
}

/**
 * Whole seconds in [millis], truncated. Stored durations are whole seconds; display
 * rounding lives in [formatMillis] and is a different concern.
 */
fun wholeSeconds(millis: Long): Int {
  require(millis >= 0L) { "millis must not be negative" }
  return (millis / MILLIS_PER_SECOND).toInt()
}

/** Splits [millis] into the minutes and seconds shown in the configuration form. */
fun durationParts(millis: Long): DurationParts {
  val totalSeconds = wholeSeconds(millis)
  return DurationParts(
    minutes = totalSeconds / SECONDS_PER_MINUTE,
    seconds = totalSeconds % SECONDS_PER_MINUTE,
  )
}

/** Rebuilds milliseconds from the parts a person edited. */
fun millisFromParts(
  minutes: Int,
  seconds: Int,
): Long {
  require(minutes >= 0) { "minutes must not be negative" }
  require(seconds in 0..MAX_SECOND_PART) { "seconds must be in 0..59" }
  return (minutes * SECONDS_PER_MINUTE_LONG + seconds) * MILLIS_PER_SECOND
}
