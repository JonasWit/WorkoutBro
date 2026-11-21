package com.wtk_apps.workoutbro.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wtk_apps.workoutbro.AppGraph
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.timer.TimerConfig
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
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], shadows = [CountingNotificationManagerShadow::class])
class TimerServiceTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    CountingNotificationManagerShadow.notifyCalls = 0
    AppGraph.timer.reset()
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
