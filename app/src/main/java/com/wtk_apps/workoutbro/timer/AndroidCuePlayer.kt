package com.wtk_apps.workoutbro.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.wtk_apps.workoutbro.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays timer cues from bundled raw sounds using [SoundPool] and drives the
 * system vibrator.
 *
 * Sounds are played on the media stream (which users normally keep audible)
 * rather than the alarm stream, so cues are reliably heard during training.
 *
 * The sequence for each cue comes from [cuePlan]; this class only turns steps into
 * Android calls, which keeps the behaviour testable off-device.
 */
class AndroidCuePlayer(
  context: Context,
) : CuePlayer {
  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val soundPool: SoundPool
  private val beepId: Int
  private val gongId: Int

  @Volatile private var loaded = false

  private val vibrator: Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      appContext.getSystemService(Context.VIBRATOR_SERVICE)
        as? Vibrator
    }

  init {
    val attributes =
      AudioAttributes
        .Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    soundPool =
      SoundPool
        .Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(attributes)
        .build()
    soundPool.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded = true }
    beepId = soundPool.load(appContext, R.raw.beep, 1)
    gongId = soundPool.load(appContext, R.raw.gong, 1)
  }

  override fun play(
    cue: TimerCue,
    config: TimerConfig,
  ) {
    val plan = cuePlan(cue)
    val firstWait = plan.indexOfFirst { it is CueStep.Wait }

    if (firstWait < 0) {
      plan.forEach { execute(it, config) }
      return
    }

    // Execute everything before the first pause immediately, so the reaction starts
    // with the cue, then continue the rest on a coroutine.
    plan.take(firstWait).forEach { execute(it, config) }
    scope.launch {
      for (index in firstWait until plan.size) {
        val step = plan[index]
        if (step is CueStep.Wait) delay(step.millis) else execute(step, config)
      }
    }
  }

  private fun execute(
    step: CueStep,
    config: TimerConfig,
  ) {
    when (step) {
      is CueStep.Sound -> sound(step.sound, config)
      is CueStep.Vibrate -> vibrate(step.millis, config)
      is CueStep.Wait -> Unit
    }
  }

  /**
   * Playback failures are swallowed on purpose: a broken audio route must never interrupt
   * or crash an ongoing workout, and there is no recovery worth surfacing to the user.
   */
  private fun sound(
    kind: CueSound,
    config: TimerConfig,
  ) {
    if (!config.soundEnabled) return
    val id = if (kind == CueSound.BEEP) beepId else gongId
    if (loaded) {
      runCatching { soundPool.play(id, VOLUME, VOLUME, 1, 0, 1f) }
    } else {
      // The very first cue may arrive before SoundPool finished loading; retry once.
      scope.launch {
        delay(150L)
        if (loaded) runCatching { soundPool.play(id, VOLUME, VOLUME, 1, 0, 1f) }
      }
    }
  }

  private fun vibrate(
    durationMs: Long,
    config: TimerConfig,
  ) {
    if (!config.vibrationEnabled) return
    val v = vibrator ?: return
    if (!v.hasVibrator()) return
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        v.vibrate(durationMs)
      }
    }
  }

  private companion object {
    const val MAX_STREAMS = 4
    const val VOLUME = 1.0f
  }
}
