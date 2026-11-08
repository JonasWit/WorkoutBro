package com.wtk_apps.workoutbro.ui.trainings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.data.MAX_PRESETS
import com.wtk_apps.workoutbro.theme.AccentGold
import com.wtk_apps.workoutbro.theme.TextPrimary
import com.wtk_apps.workoutbro.theme.TextSecondary
import com.wtk_apps.workoutbro.theme.WorkRedOrange
import com.wtk_apps.workoutbro.ui.PresetsViewModel

@Composable
fun TrainingsScreen(
  onBack: () -> Unit,
  onEdit: (Int) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PresetsViewModel = viewModel(factory = PresetsViewModel.factory()),
) {
  val presets by viewModel.presets.collectAsStateWithLifecycle()
  var showSaveDialog by remember { mutableStateOf(false) }
  var nameInput by remember { mutableStateOf("") }
  var limitReached by remember { mutableStateOf(false) }
  var actionsFor by remember { mutableStateOf<Int?>(null) }
  var pendingDelete by remember { mutableStateOf<Int?>(null) }

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
        stringResource(R.string.title_trainings),
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
      )
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onBack) { Text(stringResource(R.string.action_back), color = TextSecondary) }
    }

    Spacer(Modifier.height(8.dp))
    Text(
      stringResource(R.string.label_saved_count, presets.size, MAX_PRESETS),
      color = TextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(16.dp))
    Button(
      onClick = {
        nameInput = "Workout ${presets.size + 1}"
        showSaveDialog = true
      },
      enabled = presets.size < MAX_PRESETS,
      modifier = Modifier.fillMaxWidth().height(52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
      Text(stringResource(R.string.action_save_current_workout), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }

    if (limitReached) {
      Spacer(Modifier.height(8.dp))
      Text(stringResource(R.string.label_limit_reached, MAX_PRESETS), color = TextSecondary, fontSize = 13.sp)
    }

    Spacer(Modifier.height(10.dp))
    BackupSection(presets = presets, viewModel = viewModel)

    Spacer(Modifier.height(16.dp))
    if (presets.isEmpty()) {
      Text(stringResource(R.string.label_no_trainings), color = TextSecondary)
    }
    presets.forEachIndexed { index, preset ->
      PresetRow(preset = preset, onClick = { actionsFor = index })
      Spacer(Modifier.height(10.dp))
    }
  }

  if (showSaveDialog) {
    AlertDialog(
      onDismissRequest = { showSaveDialog = false },
      title = { Text(stringResource(R.string.dialog_save_training)) },
      text = {
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          singleLine = true,
          label = { Text(stringResource(R.string.label_name)) },
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val name = nameInput.trim().ifBlank { "Workout ${presets.size + 1}" }
          viewModel.saveCurrent(name) { added -> limitReached = !added }
          showSaveDialog = false
        }) {
          Text(stringResource(R.string.action_save))
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  val actionIndex = actionsFor
  if (actionIndex != null) {
    val preset = presets.getOrNull(actionIndex)
    Dialog(onDismissRequest = { actionsFor = null }) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = preset?.name ?: "Training",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(18.dp))
          DialogAction("LOAD", filled = true, accent = AccentGold, modifier = Modifier.fillMaxWidth()) {
            preset?.let {
              viewModel.apply(it)
              onBack()
            }
            actionsFor = null
          }
          Spacer(Modifier.height(10.dp))
          DialogAction("EDIT", filled = false, accent = AccentGold, modifier = Modifier.fillMaxWidth()) {
            actionsFor = null
            onEdit(actionIndex)
          }
          Spacer(Modifier.height(10.dp))
          DialogAction("DELETE", filled = false, accent = WorkRedOrange, modifier = Modifier.fillMaxWidth()) {
            actionsFor = null
            pendingDelete = actionIndex
          }
          Spacer(Modifier.height(4.dp))
          TextButton(onClick = { actionsFor = null }) {
            Text(stringResource(R.string.action_cancel), color = TextSecondary)
          }
        }
      }
    }
  }

  val deleteIndex = pendingDelete
  if (deleteIndex != null) {
    val preset = presets.getOrNull(deleteIndex)
    Dialog(onDismissRequest = { pendingDelete = null }) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = stringResource(R.string.dialog_delete_training),
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = preset?.name ?: "",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(18.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogAction("NO", filled = false, accent = TextSecondary, modifier = Modifier.weight(1f)) {
              pendingDelete = null
            }
            DialogAction("YES", filled = true, accent = WorkRedOrange, modifier = Modifier.weight(1f)) {
              pendingDelete = null
              viewModel.delete(deleteIndex)
            }
          }
        }
      }
    }
  }
}
