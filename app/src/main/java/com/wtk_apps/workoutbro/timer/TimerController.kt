package com.wtk_apps.workoutbro.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives the pure [TimerEngine] with elapsed time and forwards cues to a [CuePlayer].
 *
 * Both the clock and the scope are injected, so the whole loop is tested with virtual
 * time. The controller owns one session at a time; the host is responsible for creating
 * a single instance and for its lifetime.
 */
class TimerController(
  private val cuePlayer: CuePlayer,
  private val clock: MonotonicClock,
  private val scope: CoroutineScope,
  private val tickMillis: Long = DEFAULT_TICK_MILLIS,
) {
  private val _state = MutableStateFlow(TimerEngine.initialState(TimerConfig()))

  /** Observable timer state for the UI and the foreground notification. */
  val state: StateFlow<TimerState> = _state.asStateFlow()

  private var loopJob: Job? = null
  private var lastTickMillis = 0L

  /** Replaces the configuration, resetting the session unless one is running. */
  fun configure(config: TimerConfig) {
    _state.value =
      if (_state.value.running) _state.value.copy(config = config) else TimerEngine.reset(config)
    syncLoop()
  }

  fun toggle() = apply(TimerEngine.toggle(_state.value))

  fun reset() = apply(TimerTransition(TimerEngine.reset(_state.value.config)))

  fun skip() = apply(TimerEngine.skip(_state.value))

  /** Adds [deltaMillis] to the current phase (used by the +/- buttons). */
  fun adjust(deltaMillis: Long) {
    _state.value = TimerEngine.adjustRemaining(_state.value, deltaMillis)
  }

  private fun apply(transition: TimerTransition) {
    transition.cues.forEach { cuePlayer.play(it, _state.value.config) }
    _state.value = transition.state
    syncLoop()
  }

  private fun syncLoop() {
    if (_state.value.running) startLoop() else stopLoop()
  }

  private fun startLoop() {
    if (loopJob?.isActive == true) return
    lastTickMillis = clock.nowMillis()
    loopJob =
      scope.launch {
        while (isActive) {
          delay(tickMillis)
          val now = clock.nowMillis()
          val deltaMillis = now - lastTickMillis
          lastTickMillis = now
          val transition = TimerEngine.tick(_state.value, deltaMillis)
          transition.cues.forEach { cuePlayer.play(it, _state.value.config) }
          _state.value = transition.state
          if (!transition.state.running) break
        }
      }
  }

  private fun stopLoop() {
    loopJob?.cancel()
    loopJob = null
  }

  companion object {
    const val DEFAULT_TICK_MILLIS = 100L
  }
}
