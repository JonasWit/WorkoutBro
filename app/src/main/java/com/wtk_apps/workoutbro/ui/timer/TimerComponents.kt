package com.wtk_apps.workoutbro.ui.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.theme.AccentGold
import com.wtk_apps.workoutbro.theme.PrepareBlue
import com.wtk_apps.workoutbro.theme.RestGreen
import com.wtk_apps.workoutbro.theme.TextPrimary
import com.wtk_apps.workoutbro.theme.WorkYellow
import com.wtk_apps.workoutbro.theme.workProgressColor
import com.wtk_apps.workoutbro.timer.TimerPhase
import com.wtk_apps.workoutbro.timer.TimerState
import com.wtk_apps.workoutbro.timer.formatMillis
import com.wtk_apps.workoutbro.timer.totalRemainingMillis
import com.wtk_apps.workoutbro.ui.labelRes

@Composable
internal fun BottomLink(
  text: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .clip(RoundedCornerShape(50))
        .background(Color.Black.copy(alpha = 0.6f))
        .border(1.dp, TextPrimary.copy(alpha = 0.35f), RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 10.dp),
  ) {
    Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
  }
}

@Composable
internal fun PhaseHeader(
  state: TimerState,
  color: Color,
  speed: Float,
) {
  Text(
    text = stringResource(state.phase.labelRes()),
    color = color,
    fontSize = 20.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 6.sp,
  )
  Spacer(Modifier.height(10.dp))
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    ParamChip(stringResource(R.string.label_rounds), state.config.rounds.toString(), AccentGold, Modifier.weight(1f))
    ParamChip(
      stringResource(R.string.label_work),
      formatMillis(state.config.workMillis),
      WorkYellow,
      Modifier.weight(1f),
      active =
        state.phase == TimerPhase.WORK,
    )
    ParamChip(
      stringResource(R.string.label_rest),
      formatMillis(state.config.restMillis),
      RestGreen,
      Modifier.weight(1f),
      active =
        state.phase == TimerPhase.REST,
    )
    ParamChip(
      "PREP",
      formatMillis(state.config.prepareMillis),
      PrepareBlue,
      Modifier.weight(1f),
      active = state.phase == TimerPhase.PREPARE,
    )
  }
  if (state.round > 0) {
    Spacer(Modifier.height(10.dp))
    Text(
      text = stringResource(R.string.label_round_progress, state.round, state.config.rounds),
      color = Color.White,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 3.sp,
    )
  }
  Spacer(Modifier.height(14.dp))
  SessionProgressBar(
    config = state.config,
    remainingMillis = totalRemainingMillis(state),
    speed = speed,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
  )
}

/** Small parameter chip with a border in its phase colour; the active phase glows. */

@Composable
internal fun Controls(
  state: TimerState,
  actions: TimerActions,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedButton(
      onClick = actions.onReset,
      modifier = Modifier.width(92.dp),
      contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
      Text(stringResource(R.string.action_reset), maxLines = 1, softWrap = false)
    }
    val primaryLabel =
      when {
        state.running -> stringResource(R.string.action_pause)
        state.phase == TimerPhase.FINISHED -> stringResource(R.string.action_again)
        state.phase == TimerPhase.IDLE -> stringResource(R.string.action_start)
        else -> stringResource(R.string.action_resume)
      }
    Button(
      onClick = actions.onToggle,
      modifier = Modifier.weight(1f).height(56.dp),
      contentPadding = PaddingValues(horizontal = 8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
      Text(
        primaryLabel,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        maxLines = 1,
        softWrap = false,
      )
    }
    OutlinedButton(
      onClick = actions.onSkip,
      modifier = Modifier.width(92.dp),
      contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
      Text(stringResource(R.string.action_skip), maxLines = 1, softWrap = false)
    }
  }
}

@Composable
internal fun phaseColor(state: TimerState): Color {
  val target =
    when (state.phase) {
      TimerPhase.WORK -> workProgressColor(state.progress)
      TimerPhase.REST -> RestGreen
      TimerPhase.PREPARE -> PrepareBlue
      TimerPhase.IDLE, TimerPhase.FINISHED -> AccentGold
    }
  return animateColorAsState(target, tween(400), label = "phaseColor").value
}

/** Controls and navigation links pinned to the bottom of the timer screen. */
@Composable
internal fun TimerFooter(
  state: TimerState,
  actions: TimerActions,
) {
  Controls(state = state, actions = actions)
  Spacer(Modifier.height(10.dp))
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    BottomLink(stringResource(R.string.title_trainings), actions.onOpenTrainings)
    BottomLink(stringResource(R.string.title_settings), actions.onOpenSettings)
  }
  Spacer(Modifier.height(4.dp))
}

/** User actions the timer screen exposes; grouped so composables stay small. */
data class TimerActions(
  val onReset: () -> Unit,
  val onToggle: () -> Unit,
  val onSkip: () -> Unit,
  val onOpenTrainings: () -> Unit,
  val onOpenSettings: () -> Unit,
)
