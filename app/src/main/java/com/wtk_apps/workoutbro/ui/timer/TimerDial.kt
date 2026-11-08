package com.wtk_apps.workoutbro.ui.timer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtk_apps.workoutbro.theme.Surface2
import com.wtk_apps.workoutbro.theme.TextPrimary
import com.wtk_apps.workoutbro.timer.TimerState
import com.wtk_apps.workoutbro.timer.formatMillis

@Composable
internal fun ParamChip(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
  active: Boolean = false,
) {
  val transition = rememberInfiniteTransition(label = "chip")
  val pulse by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
      label = "chipPulse",
    )

  val shape = RoundedCornerShape(10.dp)
  val glowAlpha = if (active) 0.45f + 0.55f * pulse else 0f
  val borderWidth = if (active) 3.dp else 1.dp
  val borderAlpha = if (active) 0.85f + 0.15f * pulse else 0.7f
  val scale = if (active) 1f + 0.03f * pulse else 1f

  Box(
    modifier =
      modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
      },
  ) {
    if (active) {
      Box(
        modifier =
          Modifier
            .matchParentSize()
            .border(4.dp, accent.copy(alpha = glowAlpha), shape)
            .blur(13.dp),
      )
    }
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(shape)
          .background(Surface2.copy(alpha = if (active) 1f else 0.75f))
          .border(borderWidth, accent.copy(alpha = borderAlpha), shape)
          .padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
      Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
internal fun TimerDial(
  state: TimerState,
  color: Color,
  progress: Float,
  size: Dp,
) {
  Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stroke = this.size.minDimension * 0.058f
      val inset = stroke / 2f
      val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
      val topLeft = Offset(inset, inset)
      drawArc(
        color = Surface2,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
      drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )

      // Half-way marker on the ring (180 degrees from the top).
      val radius = (this.size.minDimension - stroke) / 2f
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val halfAngle = Math.toRadians(90.0) // -90° (top) + 180° sweep
      val dirX = kotlin.math.cos(halfAngle).toFloat()
      val dirY = kotlin.math.sin(halfAngle).toFloat()
      drawLine(
        color = TextPrimary.copy(alpha = 0.9f),
        start = Offset(cx + dirX * (radius - stroke * 0.55f), cy + dirY * (radius - stroke * 0.55f)),
        end = Offset(cx + dirX * (radius + stroke * 0.6f), cy + dirY * (radius + stroke * 0.6f)),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
    Text(
      text = formatMillis(state.remainingMillis),
      color = color,
      fontSize = (size.value * 0.28f).sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
    )
  }
}
