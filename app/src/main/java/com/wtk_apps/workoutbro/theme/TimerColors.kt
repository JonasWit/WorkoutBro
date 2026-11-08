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
  return if (p < 0.5f) {
    lerpHsv(WorkYellow, WorkOrange, p / 0.5f)
  } else {
    lerpHsv(WorkOrange, WorkRedOrange, (p - 0.5f) / 0.5f)
  }
}

private fun lerpHsv(
  from: Color,
  to: Color,
  t: Float,
): Color {
  val start = FloatArray(3)
  val end = FloatArray(3)
  android.graphics.Color.colorToHSV(from.toArgb(), start)
  android.graphics.Color.colorToHSV(to.toArgb(), end)

  var deltaHue = end[0] - start[0]
  if (deltaHue > 180f) deltaHue -= 360f
  if (deltaHue < -180f) deltaHue += 360f

  val hue = (start[0] + deltaHue * t + 360f) % 360f
  val saturation = start[1] + (end[1] - start[1]) * t
  val value = start[2] + (end[2] - start[2]) * t
  return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
}
