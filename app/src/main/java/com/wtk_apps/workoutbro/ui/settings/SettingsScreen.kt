package com.wtk_apps.workoutbro.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.theme.AccentGold
import com.wtk_apps.workoutbro.theme.TextPrimary
import com.wtk_apps.workoutbro.theme.TextSecondary
import com.wtk_apps.workoutbro.ui.SettingsViewModel

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory()),
) {
  val storedConfig by viewModel.config.collectAsStateWithLifecycle()
  var config by remember(storedConfig) { mutableStateOf(storedConfig) }

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
        stringResource(R.string.title_settings),
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
      )
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onBack) { Text(stringResource(R.string.action_back), color = TextSecondary) }
    }

    Spacer(Modifier.height(16.dp))

    TrainingConfigForm(config = config, onConfigChange = { config = it })

    Spacer(Modifier.height(24.dp))
    Button(
      onClick = { viewModel.save(config) { onBack() } },
      modifier = Modifier.fillMaxWidth().height(52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
      Text(stringResource(R.string.action_save_upper), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
  }
}
