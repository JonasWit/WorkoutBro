package com.wtk_apps.workoutbro.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The trainings shipped in the APK are the first thing a new user sees, so a malformed or
 * half-edited asset must be caught here instead of on the phone.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BundledTrainingsTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun bundledFileHoldsTheShippedTrainings() {
    val names = BundledPresets.read(context).map { it.name }

    assertEquals(listOf("rev plank", "side plank", "rope", "HIIT", "30x30", "rope long"), names)
  }

  @Test
  fun bundledHiitKeepsItsIntervals() {
    val hiit = BundledPresets.read(context).single { it.name == "HIIT" }

    assertEquals(15, hiit.config.rounds)
    assertEquals(30_000L, hiit.config.workMillis)
    assertEquals(30_000L, hiit.config.restMillis)
    assertEquals(20_000L, hiit.config.prepareMillis)
  }

  @Test
  fun brokenBundledFileYieldsNoTrainingsInsteadOfCrashing() {
    assertTrue(BundledPresets.parse("not a backup").isEmpty())
  }
}
