package com.wtk_apps.workoutbro.ui

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wtk_apps.workoutbro.data.MAX_PRESETS
import com.wtk_apps.workoutbro.data.PresetsBackup
import com.wtk_apps.workoutbro.data.TrainingPreset
import com.wtk_apps.workoutbro.timer.CuePlayer
import com.wtk_apps.workoutbro.timer.MonotonicClock
import com.wtk_apps.workoutbro.timer.TimerConfig
import com.wtk_apps.workoutbro.timer.TimerController
import com.wtk_apps.workoutbro.timer.TimerCue
import com.wtk_apps.workoutbro.ui.trainings.TrainingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UI checks on the JVM through Robolectric: no emulator, no installation, seconds per run.
 * They cover what unit tests cannot see — that the screen renders the saved trainings and
 * the backup actions.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TrainingsScreenTest {
  @get:Rule val compose = createComposeRule()

  private val timer =
    TimerController(
      cuePlayer =
        object : CuePlayer {
          override fun play(
            cue: TimerCue,
            config: TimerConfig,
          ) = Unit
        },
      clock = MonotonicClock { 0L },
      scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

  @Test
  fun savedTrainingsAreListedWithBackupActions() {
    val viewModel =
      PresetsViewModel(
        FakeWorkoutStore(
          initialPresets = listOf(TrainingPreset("Rano", TimerConfig(rounds = 5))),
        ),
        timer,
      )

    compose.setContent {
      TrainingsScreen(onBack = {}, onEdit = {}, viewModel = viewModel)
    }

    compose.onNodeWithText("Trainings").assertIsDisplayed()
    compose.onNodeWithText("Rano").assertIsDisplayed()
    compose.onNodeWithText("1 / $MAX_PRESETS saved").assertIsDisplayed()
    compose.onNodeWithText("EXPORT").assertIsDisplayed()
    compose.onNodeWithText("IMPORT").assertIsDisplayed()
  }

  @Test
  fun presetRow_showsTotalTimeAndTotalWorkTime() {
    // Default config: 5 rounds * 3 min work = 15:00 of work,
    // plus 4 rests of 1:00 and 0:10 prepare = 19:10 total.
    val viewModel =
      PresetsViewModel(
        FakeWorkoutStore(initialPresets = listOf(TrainingPreset("Rano", TimerConfig(rounds = 5)))),
        timer,
      )

    compose.setContent {
      TrainingsScreen(onBack = {}, onEdit = {}, viewModel = viewModel)
    }

    compose.onNodeWithText("TOTAL").assertIsDisplayed()
    compose.onNodeWithText("19:10").assertIsDisplayed()
    compose.onNodeWithText("TOTAL WORK").assertIsDisplayed()
    compose.onNodeWithText("15:00").assertIsDisplayed()
  }

  @Test
  fun emptyStore_showsTheEmptyState() {
    val viewModel = PresetsViewModel(FakeWorkoutStore(), timer)

    compose.setContent {
      TrainingsScreen(onBack = {}, onEdit = {}, viewModel = viewModel)
    }

    compose
      .onNodeWithText("No saved trainings yet. Configure the timer, then save it here.")
      .assertIsDisplayed()
  }

  @Test
  fun import_readsBackupFileOffTheMainThread() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val uri = Uri.parse("content://workoutbro.test/import.json")
    val payload = PresetsBackup.toJson(listOf(TrainingPreset("Imported", TimerConfig(rounds = 2)))).toByteArray()
    val readCalled = AtomicBoolean(false)
    val readOnMainThread = AtomicBoolean(false)
    val trackingStream: InputStream =
      object : ByteArrayInputStream(payload) {
        override fun read(
          b: ByteArray,
          off: Int,
          len: Int,
        ): Int {
          readCalled.set(true)
          if (Looper.myLooper() == Looper.getMainLooper()) readOnMainThread.set(true)
          return super.read(b, off, len)
        }
      }
    shadowOf(context.contentResolver).registerInputStream(uri, trackingStream)

    val viewModel = PresetsViewModel(FakeWorkoutStore(), timer)
    compose.setContent {
      CompositionLocalProvider(LocalActivityResultRegistryOwner provides fixedUriRegistryOwner(uri)) {
        TrainingsScreen(onBack = {}, onEdit = {}, viewModel = viewModel)
      }
    }

    compose.onNodeWithText("IMPORT").performClick()
    compose.waitUntil(timeoutMillis = 3_000L) { readCalled.get() }

    assertTrue("Backup input stream should have been read", readCalled.get())
    assertFalse("Backup import must not read the file on the main thread", readOnMainThread.get())
  }

  @Test
  fun export_writesBackupFileOffTheMainThread() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val uri = Uri.parse("content://workoutbro.test/export.json")
    val writeCalled = AtomicBoolean(false)
    val writeOnMainThread = AtomicBoolean(false)
    val trackingStream: OutputStream =
      object : ByteArrayOutputStream() {
        override fun write(
          b: ByteArray,
          off: Int,
          len: Int,
        ) {
          writeCalled.set(true)
          if (Looper.myLooper() == Looper.getMainLooper()) writeOnMainThread.set(true)
          super.write(b, off, len)
        }
      }
    shadowOf(context.contentResolver).registerOutputStream(uri, trackingStream)

    val store = FakeWorkoutStore(initialPresets = listOf(TrainingPreset("Rano", TimerConfig(rounds = 3))))
    val viewModel = PresetsViewModel(store, timer)
    compose.setContent {
      CompositionLocalProvider(LocalActivityResultRegistryOwner provides fixedUriRegistryOwner(uri)) {
        TrainingsScreen(onBack = {}, onEdit = {}, viewModel = viewModel)
      }
    }

    compose.onNodeWithText("EXPORT").performClick()
    compose.waitUntil(timeoutMillis = 3_000L) { writeCalled.get() }

    assertTrue("Backup output stream should have been written", writeCalled.get())
    assertFalse("Backup export must not write the file on the main thread", writeOnMainThread.get())
  }

  private fun fixedUriRegistryOwner(uri: Uri): ActivityResultRegistryOwner =
    object : ActivityResultRegistryOwner {
      override val activityResultRegistry: ActivityResultRegistry =
        object : ActivityResultRegistry() {
          override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
          ) {
            dispatchResult(requestCode, uri)
          }
        }
    }
}
