package com.wtk_apps.workoutbro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wtk_apps.workoutbro.theme.WorkoutBroTheme
import com.wtk_apps.workoutbro.ui.TimerViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      WorkoutBroTheme {
        val timerViewModel: TimerViewModel = viewModel(factory = TimerViewModel.factory())
        val timerState by timerViewModel.state.collectAsStateWithLifecycle()
        KeepScreenOnWhileRunning(timerState.running)
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          NotificationPermissionRequest()
          MainNavigation()
        }
      }
    }
  }
}

/** Keeps the screen awake while a training session is running. */
@Composable
private fun KeepScreenOnWhileRunning(running: Boolean) {
  val view = LocalView.current
  LaunchedEffect(running) { view.keepScreenOn = running }
  DisposableEffect(Unit) { onDispose { view.keepScreenOn = false } }
}

@Composable
private fun NotificationPermissionRequest() {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
  val context = LocalContext.current
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
  LaunchedEffect(Unit) {
    val granted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
  }
}
