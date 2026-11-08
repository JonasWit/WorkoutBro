package com.wtk_apps.workoutbro.ui

import androidx.annotation.StringRes
import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.timer.TimerPhase

/**
 * Maps a phase to the label users see. Lives in the UI layer so the timer package does
 * not know about resources; both the screen and the notification use it.
 */
@StringRes
fun TimerPhase.labelRes(): Int =
  when (this) {
    TimerPhase.IDLE -> R.string.phase_idle
    TimerPhase.PREPARE -> R.string.phase_prepare
    TimerPhase.WORK -> R.string.phase_work
    TimerPhase.REST -> R.string.phase_rest
    TimerPhase.FINISHED -> R.string.phase_finished
  }
