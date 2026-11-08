package com.wtk_apps.workoutbro.ui.trainings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.data.PresetsBackup
import com.wtk_apps.workoutbro.data.TrainingPreset
import com.wtk_apps.workoutbro.theme.AccentGold
import com.wtk_apps.workoutbro.theme.PrepareBlue
import com.wtk_apps.workoutbro.theme.RestGreen
import com.wtk_apps.workoutbro.theme.Surface1
import com.wtk_apps.workoutbro.theme.Surface2
import com.wtk_apps.workoutbro.theme.TextPrimary
import com.wtk_apps.workoutbro.theme.TextSecondary
import com.wtk_apps.workoutbro.theme.WorkYellow
import com.wtk_apps.workoutbro.timer.formatMillis
import com.wtk_apps.workoutbro.timer.totalSessionMillis
import com.wtk_apps.workoutbro.ui.PresetsViewModel

@Composable
internal fun DialogAction(
  label: String,
  filled: Boolean,
  accent: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  if (filled) {
    Button(
      onClick = onClick,
      modifier = modifier.height(50.dp),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
      Text(label, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
  } else {
    OutlinedButton(
      onClick = onClick,
      modifier = modifier.height(50.dp),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.5.dp, accent.copy(alpha = 0.85f)),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
    ) {
      Text(label, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
  }
}

@Composable
internal fun PresetRow(
  preset: TrainingPreset,
  onClick: () -> Unit,
) {
  val config = preset.config
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(Surface1, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 14.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(preset.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          stringResource(R.string.label_total),
          color = TextSecondary,
          fontSize = 8.sp,
          fontWeight = FontWeight.Medium,
          letterSpacing = 1.sp,
        )
        Text(
          formatMillis(totalSessionMillis(config)),
          color = AccentGold,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
    Spacer(Modifier.height(9.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      DetailChip("ROUNDS", config.rounds.toString(), AccentGold, Modifier.weight(1f))
      DetailChip("WORK", formatMillis(config.workMillis), WorkYellow, Modifier.weight(1f))
      DetailChip("REST", formatMillis(config.restMillis), RestGreen, Modifier.weight(1f))
      DetailChip("PREP", formatMillis(config.prepareMillis), PrepareBlue, Modifier.weight(1f))
    }
  }
}

/** Small structured chip with a border in its phase colour. */
@Composable
internal fun DetailChip(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .background(Surface2)
        .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
        .padding(horizontal = 6.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
  }
}

/** Outcome of an export or import, rendered with string resources. */
sealed interface BackupMessage {
  data class Exported(
    val count: Int,
  ) : BackupMessage

  data class Imported(
    val added: Int,
    val skipped: Int,
  ) : BackupMessage

  data object ExportFailed : BackupMessage

  data object ImportFailed : BackupMessage
}

/** Export and import of all saved trainings, plus the outcome message. */
@Composable
internal fun BackupActions(
  onExport: () -> Unit,
  onImport: () -> Unit,
  message: BackupMessage?,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TextButton(
      onClick = onExport,
      modifier = Modifier.height(32.dp),
      contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
      Text(
        stringResource(R.string.action_export),
        color = TextSecondary,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
      )
    }
    Spacer(Modifier.width(8.dp))
    TextButton(
      onClick = onImport,
      modifier = Modifier.height(32.dp),
      contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
      Text(
        stringResource(R.string.action_import),
        color = TextSecondary,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
      )
    }
  }
  when (message) {
    is BackupMessage.Exported ->
      Text(stringResource(R.string.backup_exported, message.count), color = TextSecondary, fontSize = 13.sp)
    is BackupMessage.Imported ->
      Text(
        stringResource(R.string.backup_imported, message.added, message.skipped),
        color = TextSecondary,
        fontSize = 13.sp,
      )
    BackupMessage.ExportFailed ->
      Text(stringResource(R.string.backup_export_failed), color = TextSecondary, fontSize = 13.sp)
    BackupMessage.ImportFailed ->
      Text(stringResource(R.string.backup_import_failed), color = TextSecondary, fontSize = 13.sp)
    null -> Unit
  }
}

/** Owns the export/import launchers so the screen composable stays small. */
@Composable
internal fun BackupSection(
  presets: List<TrainingPreset>,
  viewModel: PresetsViewModel,
) {
  val context = LocalContext.current
  var message by remember { mutableStateOf<BackupMessage?>(null) }

  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        val written =
          runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
              stream.write(PresetsBackup.toJson(presets).toByteArray())
            } ?: error("no output stream")
          }
        message = if (written.isSuccess) BackupMessage.Exported(presets.size) else BackupMessage.ExportFailed
      }
    }

  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val text =
          runCatching {
            context.contentResolver
              .openInputStream(uri)
              ?.bufferedReader()
              ?.use { it.readText() }
          }.getOrNull()
        val parsed = text?.let { PresetsBackup.fromJson(it) }
        if (parsed == null || parsed.isFailure) {
          message = BackupMessage.ImportFailed
        } else {
          viewModel.importPresets(parsed.getOrThrow()) { result ->
            message = BackupMessage.Imported(added = result.added, skipped = result.skipped)
          }
        }
      }
    }

  BackupActions(
    onExport = { exportLauncher.launch("workoutbro-trainings.json") },
    onImport = { importLauncher.launch(arrayOf("application/json")) },
    message = message,
  )
}
