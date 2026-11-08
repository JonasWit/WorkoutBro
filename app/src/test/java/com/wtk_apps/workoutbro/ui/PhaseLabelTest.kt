package com.wtk_apps.workoutbro.ui

import com.wtk_apps.workoutbro.R
import com.wtk_apps.workoutbro.timer.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every phase needs its own label; a new phase must not silently reuse another one. */
class PhaseLabelTest {
  @Test
  fun everyPhaseMapsToItsOwnLabel() {
    val labels = TimerPhase.entries.map { it.labelRes() }

    assertEquals(TimerPhase.entries.size, labels.toSet().size)
  }

  @Test
  fun workAndRestUseTheSharedChipLabels() {
    assertEquals(R.string.phase_work, TimerPhase.WORK.labelRes())
    assertEquals(R.string.phase_rest, TimerPhase.REST.labelRes())
  }
}
