package com.wtk_apps.workoutbro.timer

import android.content.Context
import android.media.SoundPool
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowSoundPool

/**
 * Verifies [AndroidCuePlayer] sound readiness tracking across its two independently loaded
 * SoundPool samples (beep and gong).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], shadows = [TrackingSoundPoolShadow::class])
class AndroidCuePlayerTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    TrackingSoundPoolShadow.reset()
  }

  @Test
  fun play_doesNotPlayGongWhenOnlyBeepHasFinishedLoading() {
    val player = AndroidCuePlayer(context)
    val loadedIds = TrackingSoundPoolShadow.loadedIds
    val beepId = loadedIds[0]
    val gongId = loadedIds[1]

    // Simulate SoundPool finishing only the first sample (beep), while gong is still loading.
    TrackingSoundPoolShadow.completeLoad(beepId)

    player.play(TimerCue.WORK_START, TimerConfig(soundEnabled = true, vibrationEnabled = false))

    assertFalse(
      "Gong should not be played before its own sample finishes loading",
      TrackingSoundPoolShadow.playedIds.contains(gongId),
    )
  }

  @Test
  fun play_playsGongOnceGongHasFinishedLoading() {
    val player = AndroidCuePlayer(context)
    val gongId = TrackingSoundPoolShadow.loadedIds[1]

    TrackingSoundPoolShadow.completeLoad(gongId)
    player.play(TimerCue.WORK_START, TimerConfig(soundEnabled = true, vibrationEnabled = false))

    assertTrue(
      "Gong should play once its own sample has finished loading",
      TrackingSoundPoolShadow.playedIds.contains(gongId),
    )
  }
}

@Implements(SoundPool::class)
class TrackingSoundPoolShadow : ShadowSoundPool() {
  @Implementation
  override fun load(
    context: Context?,
    resId: Int,
    priority: Int,
  ): Int {
    val id = nextId++
    loadedIds += id
    return id
  }

  @Implementation
  override fun setOnLoadCompleteListener(listener: SoundPool.OnLoadCompleteListener?) {
    loadListener = listener
  }

  @Implementation
  @Suppress("LongParameterList", "UnusedParameter")
  fun play(
    soundID: Int,
    leftVolume: Float,
    rightVolume: Float,
    priority: Int,
    loop: Int,
    rate: Float,
  ): Int {
    playedIds += soundID
    return 1
  }

  companion object {
    private var nextId = 1
    private var loadListener: SoundPool.OnLoadCompleteListener? = null
    val loadedIds = mutableListOf<Int>()
    val playedIds = mutableListOf<Int>()

    fun reset() {
      nextId = 1
      loadListener = null
      loadedIds.clear()
      playedIds.clear()
    }

    fun completeLoad(sampleId: Int) {
      loadListener?.onLoadComplete(null, sampleId, 0)
    }
  }
}
