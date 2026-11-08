package com.wtk_apps.workoutbro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wtk_apps.workoutbro.AppGraph
import com.wtk_apps.workoutbro.MainActivity
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.timer.TimerState
import com.wtk_apps.workoutbro.timer.formatMillis
import com.wtk_apps.workoutbro.ui.labelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the training timer alive (with an ongoing
 * notification and a partial wake lock) while the screen is off.
 */
class TimerService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var collectJob: Job? = null
  private var wakeLock: PowerManager.WakeLock? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel()
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    val notification = buildNotification(AppGraph.timer.state.value)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
    acquireWakeLock()
    if (collectJob == null) {
      collectJob =
        scope.launch {
          AppGraph.timer.state.collectLatest { updateNotification(it) }
        }
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    collectJob?.cancel()
    scope.cancel()
    releaseWakeLock()
    super.onDestroy()
  }

  private fun updateNotification(state: TimerState) {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    manager.notify(NOTIFICATION_ID, buildNotification(state))
  }

  private fun buildNotification(state: TimerState): Notification {
    val round = if (state.round > 0) "  •  Round ${state.round}/${state.config.rounds}" else ""
    val text = "${getString(state.phase.labelRes())}$round"
    val title = formatMillis(state.remainingMillis)

    val contentIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat
      .Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_timer)
      .setContentTitle(title)
      .setContentText(text)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
      .setContentIntent(contentIntent)
      .build()
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel() {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = getString(R.string.notification_channel_description)
        setShowBadge(false)
      }
    manager.createNotificationChannel(channel)
  }

  private fun acquireWakeLock() {
    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
    if (wakeLock?.isHeld == true) return
    wakeLock =
      powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WorkoutBro::Timer").apply {
        setReferenceCounted(false)
        @Suppress("WakelockTimeout")
        acquire()
      }
  }

  private fun releaseWakeLock() {
    wakeLock?.let { if (it.isHeld) it.release() }
    wakeLock = null
  }

  companion object {
    private const val CHANNEL_ID = "workoutbro_timer"
    private const val NOTIFICATION_ID = 1001

    fun start(context: Context) {
      ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, TimerService::class.java))
    }
  }
}
