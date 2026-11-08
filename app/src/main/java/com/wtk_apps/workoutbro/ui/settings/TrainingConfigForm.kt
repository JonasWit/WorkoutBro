package com.wtk_apps.workoutbro.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.theme.TextSecondary
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.durationParts

/**
 * The session/cues form, shared between the global Settings screen and the
 * per-training edit screen. It is a pure controlled component: it renders
 * [config] and reports edits through [onConfigChange].
 *
 * Time settings use separate minutes/seconds fields; tapping a field opens a
 * modal where the value can be typed.
 */
@Composable
internal fun TrainingConfigForm(
  config: TimerConfig,
  onConfigChange: (TimerConfig) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    SectionTitle(stringResource(R.string.section_session))

    SingleNumberField(stringResource(R.string.field_rounds), config.rounds, 1..99) {
      onConfigChange(config.copy(rounds = it))
    }

    val work = durationParts(config.workMillis)
    DurationFields(
      label = stringResource(R.string.field_work),
      parts = work,
      minimumMillis = 1_000L,
      onChange = { onConfigChange(config.copy(workMillis = it)) },
    )

    val rest = durationParts(config.restMillis)
    DurationFields(
      label = stringResource(R.string.field_rest),
      parts = rest,
      minimumMillis = 0L,
      onChange = { onConfigChange(config.copy(restMillis = it)) },
    )

    val prepare = durationParts(config.prepareMillis)
    DurationFields(
      label = stringResource(R.string.field_prepare),
      parts = prepare,
      minimumMillis = 0L,
      onChange = { onConfigChange(config.copy(prepareMillis = it)) },
    )

    Spacer(Modifier.height(16.dp))
    SectionTitle(stringResource(R.string.section_cues))
    SingleNumberField(stringResource(R.string.field_countdown), config.countdownSeconds, 0..10) {
      onConfigChange(config.copy(countdownSeconds = it))
    }
    Toggle(stringResource(R.string.toggle_half_way), config.halfWayEnabled) {
      onConfigChange(config.copy(halfWayEnabled = it))
    }
    Toggle(stringResource(R.string.toggle_countdown), config.countdownEnabled) {
      onConfigChange(config.copy(countdownEnabled = it))
    }
    Toggle(stringResource(R.string.toggle_sound), config.soundEnabled) {
      onConfigChange(config.copy(soundEnabled = it))
    }
    Toggle(stringResource(R.string.toggle_vibration), config.vibrationEnabled) {
      onConfigChange(config.copy(vibrationEnabled = it))
    }
  }
}

@Composable
internal fun SectionTitle(text: String) {
  Text(
    text = text.uppercase(),
    color = TextSecondary,
    fontSize = 12.sp,
    letterSpacing = 3.sp,
    fontWeight = FontWeight.Medium,
  )
  Spacer(Modifier.height(8.dp))
}

/** A single number (no minutes/seconds split), edited through a modal. */
