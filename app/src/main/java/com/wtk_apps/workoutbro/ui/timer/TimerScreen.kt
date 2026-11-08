package com.wtk_apps.workoutbro.ui.timer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.service.TimerService
import com.wtk_apps.workoutbro.theme.AccentGold
import com.wtk_apps.workoutbro.theme.Ink
import com.wtk_apps.workoutbro.theme.PrepareBlue
import com.wtk_apps.workoutbro.theme.RestGreen
import com.wtk_apps.workoutbro.theme.TextPrimary
import com.wtk_apps.workoutbro.theme.WorkYellow
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerPhase
import com.wtk_apps.workoutbro.timer.sessionSegments
import com.wtk_apps.workoutbro.ui.AppVersion
import com.wtk_apps.workoutbro.ui.TimerViewModel
import com.wtk_apps.workoutbro.ui.effects.FlameBackground

/** Roman-inscription style typeface used for the app title. */
private val RomanTitleFont = FontFamily(Font(R.font.cinzel))

@Composable
fun TimerScreen(
  onOpenSettings: () -> Unit,
  onOpenTrainings: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TimerViewModel = viewModel(factory = TimerViewModel.factory()),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // Keep the timer alive with a foreground service while it is running.
  LaunchedEffect(state.running) {
    if (state.running) TimerService.start(context) else TimerService.stop(context)
  }

  val phaseColor = phaseColor(state)
  // Flames move at a different pace per phase: brisk on work, medium on prepare
  // and slow/calm on rest.
  val flameSpeed =
    when (state.phase) {
      TimerPhase.WORK -> 2.6f
      TimerPhase.PREPARE -> 1.2f
      TimerPhase.REST -> 0.55f
      TimerPhase.IDLE, TimerPhase.FINISHED -> 0.8f
    }
  val ringProgress by
    animateFloatAsState(
      targetValue = state.progress,
      animationSpec = tween(300, easing = LinearEasing),
      label = "ring",
    )

  Box(modifier = modifier.fillMaxSize()) {
    FlameBackground(color = phaseColor, speed = flameSpeed)

    Column(
      modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      TimerTitle()

      // Phase label + dial, centred in the space between title and controls.
      BoxWithConstraints(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        val dialSize = minOf(maxWidth.value, (maxHeight.value - 150f).coerceAtLeast(170f)).dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          PhaseHeader(state = state, color = phaseColor, speed = flameSpeed)
          Spacer(Modifier.height(18.dp))
          TimerDial(state = state, color = phaseColor, progress = ringProgress, size = dialSize)
        }
      }

      TimerFooter(
        state = state,
        actions =
          TimerActions(
            onReset = viewModel::reset,
            onToggle = viewModel::toggle,
            onSkip = viewModel::skip,
            onOpenTrainings = onOpenTrainings,
            onOpenSettings = onOpenSettings,
          ),
      )
    }
  }
}

/** Segmented session bar: PREPARE / WORK / REST sections split by dividers. */
@Composable
internal fun SessionProgressBar(
  config: TimerConfig,
  remainingMillis: Long,
  speed: Float,
  modifier: Modifier = Modifier,
) {
  val segments = remember(config) { sessionSegments(config) }
  val total = remember(segments) { segments.sumOf { it.millis }.coerceAtLeast(1L) }
  val target = ((total - remainingMillis).toFloat() / total).coerceIn(0f, 1f)
  val fill by animateFloatAsState(target, tween(400, easing = LinearEasing), label = "sessionFill")

  // Fire-like shimmer that sweeps across the bar; faster while working.
  val shimmer = remember { mutableFloatStateOf(0f) }
  val currentSpeed = rememberUpdatedState(speed)
  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        if (last != 0L) {
          val dt = (now - last) / 1_000_000_000f
          shimmer.floatValue = (shimmer.floatValue + dt * (0.22f + currentSpeed.value * 0.20f)) % 1f
        }
        last = now
      }
    }
  }

  Canvas(modifier = modifier.height(18.dp)) {
    val w = size.width
    val h = size.height
    val radius = h * 0.25f
    val bar = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(radius, radius))) }
    val playX = w * fill

    clipPath(bar) {
      var x = 0f
      segments.forEachIndexed { i, seg ->
        val sw = w * (seg.millis.toFloat() / total)
        val c = segmentColor(seg.phase)
        // Dim track for the whole section.
        drawRect(c.copy(alpha = 0.22f), topLeft = Offset(x, 0f), size = Size(sw, h))
        // Elapsed part: flickering, glossy colour like the flames.
        val filled = (minOf(x + sw, playX) - x).coerceAtLeast(0f)
        if (filled > 0f) {
          val flick = 0.5f + 0.5f * kotlin.math.sin(shimmer.floatValue * 6.2831855f * (1f + (i % 3)) + i * 1.7f)
          val bright = lerp(c, Color.White, 0.10f + 0.28f * flick)
          drawRect(
            brush = Brush.verticalGradient(listOf(lerp(bright, Color.White, 0.20f), bright, c), startY = 0f, endY = h),
            topLeft = Offset(x, 0f),
            size = Size(filled, h),
          )
        }
        x += sw
      }
    }

    // Dividers between sections.
    var dividerX = 0f
    for (i in 0 until segments.size - 1) {
      dividerX += w * (segments[i].millis.toFloat() / total)
      drawRect(Ink, topLeft = Offset(dividerX - 1.5.dp.toPx(), 0f), size = Size(3.dp.toPx(), h))
    }

    // Moving shimmer highlight (like the flames).
    val bandHalf = w * 0.20f
    val bandCenter = (shimmer.floatValue * 1.6f - 0.3f) * w
    clipPath(bar) {
      drawRect(
        brush =
          Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
            startX = bandCenter - bandHalf,
            endX = bandCenter + bandHalf,
          ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
      )
    }

    // Animated playhead.
    drawLine(
      color = TextPrimary,
      start = Offset(playX, -2.dp.toPx()),
      end = Offset(playX, h + 2.dp.toPx()),
      strokeWidth = 3.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}

private fun segmentColor(phase: TimerPhase): Color =
  when (phase) {
    TimerPhase.PREPARE -> PrepareBlue
    TimerPhase.REST -> RestGreen
    else -> WorkYellow
  }

/** App title with the version of the installed build underneath. */
@Composable
private fun TimerTitle() {
  Text(
    text = stringResource(R.string.title_workout_bro),
    color = AccentGold,
    fontFamily = RomanTitleFont,
    fontSize = 22.sp,
    letterSpacing = 3.sp,
    modifier = Modifier.padding(top = 8.dp),
  )
  AppVersion()
}
