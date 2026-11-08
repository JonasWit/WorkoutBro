package com.wtk_apps.workoutbro.timer

/** A duration split into the parts a person edits: whole minutes and seconds 0..59. */
data class DurationParts(
  val minutes: Int,
  val seconds: Int,
) {
  init {
    require(minutes >= 0) { "minutes must not be negative" }
    require(seconds in 0..59) { "seconds must be in 0..59" }
  }

  val totalSeconds: Int
    get() = minutes * 60 + seconds
}

/**
 * Whole seconds in [millis], truncated. Stored durations are whole seconds; display
 * rounding lives in [formatMillis] and is a different concern.
 */
fun wholeSeconds(millis: Long): Int {
  require(millis >= 0L) { "millis must not be negative" }
  return (millis / 1_000L).toInt()
}

/** Splits [millis] into the minutes and seconds shown in the configuration form. */
fun durationParts(millis: Long): DurationParts {
  val totalSeconds = wholeSeconds(millis)
  return DurationParts(minutes = totalSeconds / 60, seconds = totalSeconds % 60)
}

/** Rebuilds milliseconds from the parts a person edited. */
fun millisFromParts(
  minutes: Int,
  seconds: Int,
): Long {
  require(minutes >= 0) { "minutes must not be negative" }
  require(seconds in 0..59) { "seconds must be in 0..59" }
  return (minutes * 60L + seconds) * 1_000L
}
