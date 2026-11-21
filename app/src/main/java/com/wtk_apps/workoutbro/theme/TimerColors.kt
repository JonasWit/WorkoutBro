package com.wtk_apps.workoutbro.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Smooth colour for a work round as it elapses: gold/yellow at the start,
 * orange in the middle and deep orange-red at the end. Interpolation happens in
 * HSV space so the midpoints stay vivid instead of turning muddy grey.
 */
fun workProgressColor(progress: Float): Color {
  val p = progress.coerceIn(0f, 1f)
  return if (p < MIDPOINT) {
    lerpHsv(WorkYellow, WorkOrange, p / MIDPOINT)
  } else {
    lerpHsv(WorkOrange, WorkRedOrange, (p - MIDPOINT) / MIDPOINT)
  }
}

private fun lerpHsv(
  from: Color,
  to: Color,
  t: Float,
): Color {
  val start = FloatArray(HSV_CHANNELS)
  val end = FloatArray(HSV_CHANNELS)
  android.graphics.Color.colorToHSV(from.toArgb(), start)
  android.graphics.Color.colorToHSV(to.toArgb(), end)

  var deltaHue = end[0] - start[0]
  if (deltaHue > HALF_CIRCLE_DEGREES) deltaHue -= FULL_CIRCLE_DEGREES
  if (deltaHue < -HALF_CIRCLE_DEGREES) deltaHue += FULL_CIRCLE_DEGREES

  val hue = (start[0] + deltaHue * t + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES
  val saturation = start[1] + (end[1] - start[1]) * t
  val value = start[2] + (end[2] - start[2]) * t
  return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
}

private const val MIDPOINT = 0.5f
private const val HSV_CHANNELS = 3
private const val HALF_CIRCLE_DEGREES = 180f
private const val FULL_CIRCLE_DEGREES = 360f
