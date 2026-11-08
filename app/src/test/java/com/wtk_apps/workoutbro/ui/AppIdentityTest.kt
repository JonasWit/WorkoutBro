package com.wtk_apps.workoutbro.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wtk_apps.workoutbro.BuildConfig
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.data.settingsRepository
import com.wtk_apps.workoutbro.timer.TimerConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The installed build, its launcher label and its settings storage must all carry the
 * WorkoutBro name. Those are the three places a rename silently misses: the build config,
 * the translatable label and the DataStore, where a leftover name means lost settings.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppIdentityTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun buildInstallsUnderTheWorkoutBroPackage() {
    assertEquals("com.wtk_apps.workoutbro", BuildConfig.APPLICATION_ID)
  }

  @Test
  fun launcherShowsTheWorkoutBroName() {
    assertEquals("WorkoutBro", context.getString(R.string.app_name))
  }

  @Test
  fun savedSettingsAreReadBackFromTheStore() =
    runTest {
      settingsRepository(context).save(TimerConfig(rounds = 5))

      assertEquals(5, settingsRepository(context).config.first().rounds)
    }
}
