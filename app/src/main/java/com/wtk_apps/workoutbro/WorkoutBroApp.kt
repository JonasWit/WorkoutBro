package com.wtk_apps.workoutbro

import android.app.Application
import com.wtk_apps.workoutbro.data.BundledPresets
import com.wtk_apps.workoutbro.data.SettingsRepository
import com.wtk_apps.workoutbro.data.settingsRepository
import com.wtk_apps.workoutbro.timer.AndroidCuePlayer
import com.wtk_apps.workoutbro.timer.MonotonicClock
import com.wtk_apps.workoutbro.timer.TimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Process-wide wiring. Exists only because the app has no dependency-injection setup;
 * every dependency here is created once, in one place, and must not be re-assigned later.
 */
object AppGraph {
  lateinit var settings: SettingsRepository
    private set

  lateinit var timer: TimerController
    private set

  fun init(context: Application) {
    init(
      settings = settingsRepository(context),
      timer =
        TimerController(
          cuePlayer = AndroidCuePlayer(context),
          clock = MonotonicClock { System.nanoTime() / NANOS_PER_MILLI },
          scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        ),
    )
  }

  private const val NANOS_PER_MILLI = 1_000_000L

  internal fun init(
    settings: SettingsRepository,
    timer: TimerController,
  ) {
    this.settings = settings
    this.timer = timer
  }
}

class WorkoutBroApp : Application() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    AppGraph.init(this)
    scope.launch {
      AppGraph.settings.seedDefaults(BundledPresets.read(this@WorkoutBroApp))
      val config = AppGraph.settings.config.first()
      AppGraph.timer.configure(config)
    }
  }
}
