package com.wtk_apps.workoutbro.service

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wtk_apps.workoutbro.AppGraph
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.data.settingsRepository
import com.wtk_apps.workoutbro.timer.CuePlayer
import com.wtk_apps.workoutbro.timer.MonotonicClock
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import com.wtk_apps.workoutbro.timer.TimerCue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowNotificationManager

/**
 * Verifies the foreground timer service lifecycle, notification throttling and string
 * formatting without a physical device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, shadows = [CountingNotificationManagerShadow::class])
class TimerServiceTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var scheduler: TestCoroutineScheduler

  @Before
  fun setUp() {
    CountingNotificationManagerShadow.notifyCalls = 0
    scheduler = TestCoroutineScheduler()
    val testController =
      TimerController(
        cuePlayer =
          object : CuePlayer {
            override fun play(
              cue: TimerCue,
              config: TimerConfig,
            ) = Unit
          },
        clock = MonotonicClock { scheduler.currentTime },
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scheduler)),
      )
    AppGraph.init(settingsRepository(context), testController)
  }

  @After
  fun tearDown() {
    AppGraph.timer.reset()
  }

  @Test
  fun notificationRoundResource_formatsCurrentAndTotalRounds() {
    assertEquals("Round 2/5", context.getString(R.string.notification_round, 2, 5))
  }

  @Test
  fun timerStopping_stopsForegroundServiceItself() {
    val cfg = TimerConfig(rounds = 1, workMillis = 60_000L, restMillis = 0L, prepareMillis = 0L)
    AppGraph.timer.configure(cfg)
    AppGraph.timer.toggle()

    val controller = Robolectric.buildService(TimerService::class.java).create().startCommand(0, 0)
    shadowOf(Looper.getMainLooper()).idle()

    AppGraph.timer.reset()
    shadowOf(Looper.getMainLooper()).idle()

    val stopped = shadowOf(controller.get()).isStoppedBySelf
    assertTrue("TimerService should stop itself once the timer is no longer running", stopped)
    controller.destroy()
  }

  @Test
  fun subSecondUpdatesWithIdenticalDisplay_doNotSpamNotificationManager() {
    val cfg = TimerConfig(rounds = 1, workMillis = 60_000L, restMillis = 0L, prepareMillis = 0L)
    AppGraph.timer.configure(cfg)
    AppGraph.timer.toggle()

    val controller = Robolectric.buildService(TimerService::class.java).create().startCommand(0, 0)
    shadowOf(Looper.getMainLooper()).idle()
    val callsAfterStart = CountingNotificationManagerShadow.notifyCalls

    // Five 50ms drops keep remainingMillis in 59_750..60_000, which still displays as "01:00".
    repeat(5) {
      AppGraph.timer.adjust(-50L)
      shadowOf(Looper.getMainLooper()).idle()
    }

    assertEquals(
      "Notification should not be re-posted when displayed MM:SS, phase and round did not change",
      callsAfterStart,
      CountingNotificationManagerShadow.notifyCalls,
    )

    controller.destroy()
  }
}

@Implements(NotificationManager::class)
class CountingNotificationManagerShadow : ShadowNotificationManager() {
  @Implementation
  override fun notify(
    tag: String?,
    id: Int,
    notification: Notification?,
  ) {
    notifyCalls++
    super.notify(tag, id, notification)
  }

  companion object {
    var notifyCalls: Int = 0
  }
}
