package com.wtk_apps.workoutbro.ui.trainings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.theme.AccentGold
import com.wtk_apps.workoutbro.theme.TextPrimary
import com.wtk_apps.workoutbro.theme.TextSecondary
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.ui.PresetsViewModel
import com.wtk_apps.workoutbro.ui.settings.TrainingConfigForm

/** Edits the saved training at [index] (name + parameters) and saves it back. */
@Composable
fun EditTrainingScreen(
  index: Int,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PresetsViewModel = viewModel(factory = PresetsViewModel.factory()),
) {
  val presets by viewModel.presets.collectAsStateWithLifecycle()

  var loaded by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf("") }
  var config by remember { mutableStateOf(TimerConfig()) }

  val preset = presets.getOrNull(index)
  LaunchedEffect(preset) {
    if (!loaded && preset != null) {
      name = preset.name
      config = preset.config
      loaded = true
    }
  }
  // The training was deleted while editing.
  LaunchedEffect(presets.size, loaded) {
    if (loaded && presets.getOrNull(index) == null) onBack()
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .safeDrawingPadding()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(
        stringResource(R.string.title_edit_training),
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
      )
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onBack) {
        Text(stringResource(R.string.action_back), color = TextSecondary)
      }
    }

    Spacer(Modifier.height(16.dp))
    NameField(name = name, onNameChange = { name = it })

    Spacer(Modifier.height(16.dp))
    TrainingConfigForm(config = config, onConfigChange = { config = it })

    Spacer(Modifier.height(24.dp))
    Button(
      onClick = {
        val finalName = name.trim().ifBlank { preset?.name ?: "Workout" }
        viewModel.update(index, finalName, config)
        onBack()
      },
      modifier = Modifier.fillMaxWidth().height(52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
      Text(stringResource(R.string.action_save_changes), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
  }
}

@Composable
private fun NameField(
  name: String,
  onNameChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = name,
    onValueChange = onNameChange,
    singleLine = true,
    label = { Text(stringResource(R.string.label_name)) },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    modifier = Modifier.fillMaxWidth(),
  )
}
