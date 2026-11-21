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
  private val lock = Any()
  private val _state = MutableStateFlow(TimerEngine.initialState(TimerConfig()))

  /** Observable timer state for the UI and the foreground notification. */
  val state: StateFlow<TimerState> = _state.asStateFlow()

  private var loopJob: Job? = null
  private var lastTickMillis = 0L

  /** Replaces the configuration, resetting the session unless one is running. */
  fun configure(config: TimerConfig) {
    synchronized(lock) {
      _state.value =
        if (_state.value.running) _state.value.copy(config = config) else TimerEngine.reset(config)
      syncLoopLocked()
    }
  }

  fun toggle() = apply { TimerEngine.toggle(it) }

  fun reset() = apply { TimerTransition(TimerEngine.reset(it.config)) }

  fun skip() = apply { TimerEngine.skip(it) }

  /** Adds [deltaMillis] to the current phase (used by the +/- buttons). */
  fun adjust(deltaMillis: Long) {
    synchronized(lock) {
      _state.value = TimerEngine.adjustRemaining(_state.value, deltaMillis)
    }
  }

  private fun apply(transform: (TimerState) -> TimerTransition) {
    val (cues, config) =
      synchronized(lock) {
        val transition = transform(_state.value)
        _state.value = transition.state
        syncLoopLocked()
        transition.cues to transition.state.config
      }
    cues.forEach { cuePlayer.play(it, config) }
  }

  private fun syncLoopLocked() {
    if (_state.value.running) startLoopLocked() else stopLoopLocked()
  }

  private fun startLoopLocked() {
    if (loopJob?.isActive == true) return
    lastTickMillis = clock.nowMillis()
    loopJob =
      scope.launch {
        while (isActive) {
          delay(tickMillis)
          val (cues, config, running) =
            synchronized(lock) {
              val now = clock.nowMillis()
              val deltaMillis = (now - lastTickMillis).coerceAtLeast(0L)
              lastTickMillis = now
              val transition = TimerEngine.tick(_state.value, deltaMillis)
              _state.value = transition.state
              Triple(transition.cues, transition.state.config, transition.state.running)
            }
          cues.forEach { cuePlayer.play(it, config) }
          if (!running) break
        }
      }
  }

  private fun stopLoopLocked() {
    loopJob?.cancel()
    loopJob = null
  }

  companion object {
    const val DEFAULT_TICK_MILLIS = 100L
  }
}
