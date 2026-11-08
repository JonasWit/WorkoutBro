package com.wtk_apps.workoutbro.ui.effects

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush

/**
 * Natural-looking fire drawn with an AGSL fragment shader (Android 13+). On
 * older devices it falls back to the procedural flame shapes.
 */
@Composable
internal fun FlameBackground(
  color: Color,
  speed: Float,
) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    FireShaderBackground(color, speed)
  } else {
    ProceduralFlameBackground(color)
  }
}

private const val FIRE_AGSL =
  """
uniform float2 uResolution;
uniform float uTime;
uniform float uColorR;
uniform float uColorG;
uniform float uColorB;

float hash21(float2 p) {
  float3 p3 = fract(float3(p.x, p.y, p.x) * 0.1031);
  p3 += dot(p3, p3.yzx + 33.33);
  return fract((p3.x + p3.y) * p3.z);
}

float noise(float2 p) {
  float2 i = floor(p);
  float2 f = fract(p);
  float a = hash21(i);
  float b = hash21(i + float2(1.0, 0.0));
  float c = hash21(i + float2(0.0, 1.0));
  float d = hash21(i + float2(1.0, 1.0));
  float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);   // quintic (fewer axis artifacts)
  return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
  float v = 0.0;
  float amp = 0.5;
  for (int i = 0; i < 4; i++) {
    v += amp * noise(p);
    p *= 2.0;
    amp *= 0.5;
  }
  return v;
}

half4 main(float2 fragCoord) {
  float3 uColor = float3(uColorR, uColorG, uColorB);
  float2 uv = fragCoord / uResolution;
  uv.y = 1.0 - uv.y;                       // y = 0 at the bottom
  float aspect = uResolution.x / uResolution.y;

  float2 p = float2(uv.x * aspect * 2.4, uv.y * 3.0);
  p.y -= uTime * 1.2;                       // flames rise
  // Turbulence in both axes so the pattern never forms horizontal bands.
  float wx = noise(p * 0.6 + float2(uTime * 0.30, 0.0));
  float wy = noise(p * 0.6 + float2(0.0, uTime * 0.45) + 31.0);
  float n = fbm(p + float2((wx - 0.5) * 1.7, (wy - 0.5) * 1.3));

  // Fire hugs the bottom edge and fades out towards the top.
  float heightMask = smoothstep(0.9, 0.05, uv.y);
  float fire = pow(n * heightMask, 1.7) * 1.5;

  float t = clamp(fire, 0.0, 1.0);
  float3 col = uColor * t * 1.2;
  col += float3(1.0, 0.86, 0.55) * pow(t, 3.0) * 0.5;   // hot core
  col += float3(0.025, 0.023, 0.02);                     // faint ambient

  return half4(col, 1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun FireShaderBackground(
  color: Color,
  speed: Float,
) {
  // Accumulate flame time by integrating the current speed, so changing the pace
  // (e.g. entering a work round) never makes the flames jump.
  val fireTime = remember { mutableFloatStateOf(0f) }
  val currentSpeed = rememberUpdatedState(speed)
  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        if (last != 0L) {
          fireTime.floatValue += ((now - last) / 1_000_000_000f) * currentSpeed.value
        }
        last = now
      }
    }
  }

  val shader = remember { RuntimeShader(FIRE_AGSL) }
  val r = color.red
  val g = color.green
  val b = color.blue

  Canvas(modifier = Modifier.fillMaxSize()) {
    shader.setFloatUniform("uResolution", size.width, size.height)
    shader.setFloatUniform("uTime", fireTime.floatValue)
    shader.setFloatUniform("uColorR", r)
    shader.setFloatUniform("uColorG", g)
    shader.setFloatUniform("uColorB", b)
    drawRect(brush = ShaderBrush(shader), size = size)
  }
}

/**
 * Animated flame tongues rising from the bottom of the screen, tinted with the
 * phase colour. Purely procedural: flickering vertical-gradient ovals combined
 * with additive blending so overlapping flames glow.
 */
@Composable
private fun ProceduralFlameBackground(color: Color) {
  val transition = rememberInfiniteTransition(label = "flames")
  val clock by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * Math.PI.toFloat()),
      animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
      label = "flameClock",
    )

  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // Warm glow along the bottom edge.
    drawRect(
      brush = Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.12f)), startY = h * 0.80f, endY = h),
      topLeft = Offset(0f, h * 0.80f),
      size = Size(w, h * 0.20f),
      blendMode = BlendMode.Plus,
    )

    val flameCount = 12
    for (i in 0 until flameCount) {
      val base = (i + 0.5f) / flameCount + (hash(i * 5.1f) - 0.5f) * 0.05f
      val speed = 0.8f + hash(i * 1.7f) * 1.4f
      val phase = hash(i * 9.3f) * 2f * Math.PI.toFloat()
      val angle = clock * speed + phase
      val flick = 0.55f + 0.45f * kotlin.math.sin(angle)
      val flameHeight = h * (0.16f + 0.18f * hash(i * 3.3f)) * (0.75f + 0.5f * flick)
      val flameWidth = w * (0.10f + 0.06f * hash(i * 2.9f))
      val lean = kotlin.math.sin(angle * 0.7f) * w * 0.03f
      val x = w * base
      val alpha = 0.10f + 0.16f * flick
      val top = h - flameHeight
      val path =
        Path().apply {
          moveTo(x - flameWidth / 2f, h)
          cubicTo(
            x - flameWidth * 0.45f,
            h - flameHeight * 0.35f,
            x - flameWidth * 0.2f + lean * 0.5f,
            top + flameHeight * 0.3f,
            x + lean,
            top,
          )
          cubicTo(
            x + flameWidth * 0.2f + lean * 0.5f,
            top + flameHeight * 0.3f,
            x + flameWidth * 0.45f,
            h - flameHeight * 0.35f,
            x + flameWidth / 2f,
            h,
          )
          close()
        }
      drawPath(
        path = path,
        brush =
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = alpha * 0.55f), color.copy(alpha = alpha)),
            startY = top,
            endY = h,
          ),
        blendMode = BlendMode.Plus,
      )
    }
  }
}

private fun hash(n: Float): Float {
  val x = kotlin.math.sin(n) * 43758.5453f
  return x - kotlin.math.floor(x)
}
