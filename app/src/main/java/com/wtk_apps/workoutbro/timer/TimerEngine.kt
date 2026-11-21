package com.wtk_apps.workoutbro.timer

/**
 * Current phase of the interval timer.
 *
 * Lifecycle: IDLE -> PREPARE? -> WORK -> REST -> WORK -> ... -> FINISHED
 */
enum class TimerPhase {
  IDLE,
  PREPARE,
  WORK,
  REST,
  FINISHED,
}

/** Audio/haptic events emitted by the engine when time crosses a threshold. */
enum class TimerCue {
  /** Short beep announcing the preparation countdown. */
  PREPARE_START,

  /** Gong announcing the start of a work round. */
  WORK_START,

  /** Gong announcing the end of a work round. */
  WORK_END,

  /** Beep at the half-way point of a work round. */
  HALF_WAY,

  /** Short beep for each of the final countdown seconds. */
  COUNTDOWN,

  /** Final celebration gong when the whole session ends. */
  FINISHED,
}

/**
 * Immutable configuration of a training session.
 *
 * All durations are in milliseconds. Instances are validated on construction.
 */
data class TimerConfig(
  val rounds: Int = 3,
  val workMillis: Long = 180_000L,
  val restMillis: Long = 60_000L,
  val prepareMillis: Long = 10_000L,
  val countdownSeconds: Int = 3,
  val halfWayEnabled: Boolean = true,
  val countdownEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true,
  val soundEnabled: Boolean = true,
) {
  init {
    require(rounds >= 1) { "rounds must be at least 1" }
    require(workMillis > 0L) { "workMillis must be positive" }
    require(restMillis >= 0L) { "restMillis must not be negative" }
    require(prepareMillis >= 0L) { "prepareMillis must not be negative" }
    require(countdownSeconds >= 0) { "countdownSeconds must not be negative" }
  }
}

/**
 * Immutable snapshot of the timer.
 *
 * @property round 1-based current round; 0 while idle or preparing.
 * @property remainingMillis time left in the current phase.
 * @property phaseMillis total duration of the current phase.
 */
data class TimerState(
  val config: TimerConfig,
  val phase: TimerPhase = TimerPhase.IDLE,
  val round: Int = 0,
  val remainingMillis: Long = 0L,
  val phaseMillis: Long = 0L,
  val running: Boolean = false,
) {
  /** Elapsed fraction of the current phase in the range 0f..1f. */
  val progress: Float
    get() =
      if (phaseMillis <= 0L) {
        0f
      } else {
        ((phaseMillis - remainingMillis).toFloat() / phaseMillis).coerceIn(0f, 1f)
      }
}

/** Result of a state change: the new state plus any cues that fired. */
data class TimerTransition(
  val state: TimerState,
  val cues: List<TimerCue> = emptyList(),
)

/**
 * Pure interval-timer state machine.
 *
 * Every function takes a [TimerState] and returns the next value; nothing here
 * touches the clock, audio, or the Android framework, which keeps it trivially
 * testable. The host (ViewModel/service) is responsible for driving [tick] with
 * elapsed time and for turning [TimerCue]s into sound and vibration.
 */
object TimerEngine {
  fun initialState(config: TimerConfig): TimerState =
    TimerState(
      config = config,
      phase = TimerPhase.IDLE,
      round = 0,
      remainingMillis = 0L,
      phaseMillis = 0L,
      running = false,
    )

  /** Starts a session, or resumes a paused one. */
  fun start(state: TimerState): TimerTransition =
    when (state.phase) {
      TimerPhase.IDLE, TimerPhase.FINISHED -> beginSession(state.config)
      else -> TimerTransition(state.copy(running = true))
    }

  /** Pauses the session; a paused session ignores [tick]. */
  fun pause(state: TimerState): TimerState =
    if (state.phase == TimerPhase.IDLE || state.phase == TimerPhase.FINISHED) state else state.copy(running = false)

  /** Pauses when running, otherwise starts/resumes. */
  fun toggle(state: TimerState): TimerTransition = if (state.running) TimerTransition(pause(state)) else start(state)

  /** Returns a fresh idle state for the given configuration. */
  fun reset(config: TimerConfig): TimerState = initialState(config)

  /** Ends the current phase immediately and moves on to the next one. */
  fun skip(state: TimerState): TimerTransition =
    if (state.phase == TimerPhase.IDLE || state.phase == TimerPhase.FINISHED) {
      TimerTransition(state)
    } else {
      advancePhase(state)
    }

  /** Adds [deltaMillis] to the current phase, clamped to [minMillis]. */
  fun adjustRemaining(
    state: TimerState,
    deltaMillis: Long,
    minMillis: Long = MILLIS_PER_SECOND,
  ): TimerState {
    if (!state.phase.isActivePhase()) return state
    val newRemaining = (state.remainingMillis + deltaMillis).coerceAtLeast(minMillis)
    return state.copy(remainingMillis = newRemaining, phaseMillis = maxOf(state.phaseMillis, newRemaining))
  }

  /**
   * Advances the timer by [deltaMillis] and reports any cues that fired.
   *
   * Handles overshoot so that a single large delta can cross several phase
   * boundaries; every crossed threshold and phase change is reflected in the
   * returned cue list.
   */
  fun tick(
    state: TimerState,
    deltaMillis: Long,
  ): TimerTransition {
    if (!state.running || !state.phase.isActivePhase()) {
      return TimerTransition(state)
    }
    require(deltaMillis >= 0L) { "deltaMillis must not be negative" }

    var current = state
    var remainingDelta = deltaMillis
    val cues = mutableListOf<TimerCue>()

    while (remainingDelta > 0L && current.phase != TimerPhase.FINISHED) {
      val before = current.remainingMillis
      if (before > remainingDelta) {
        val after = before - remainingDelta
        cues += cuesBetween(current, before, after)
        current = current.copy(remainingMillis = after)
        remainingDelta = 0L
      } else {
        cues += cuesBetween(current, before, 0L)
        val overflow = remainingDelta - before
        val advanced = advancePhase(current)
        cues += advanced.cues
        current = advanced.state
        remainingDelta = overflow
      }
    }
    return TimerTransition(current, cues)
  }
}

private const val MILLIS_PER_SECOND = 1_000L
private const val CEIL_ROUNDING_OFFSET = 999L

private fun TimerPhase.isActivePhase(): Boolean =
  when (this) {
    TimerPhase.PREPARE, TimerPhase.WORK, TimerPhase.REST -> true
    TimerPhase.IDLE, TimerPhase.FINISHED -> false
  }

private fun beginSession(config: TimerConfig): TimerTransition =
  if (config.prepareMillis > 0L) {
    TimerTransition(
      TimerState(
        config = config,
        phase = TimerPhase.PREPARE,
        round = 0,
        remainingMillis = config.prepareMillis,
        phaseMillis = config.prepareMillis,
        running = true,
      ),
      listOf(TimerCue.PREPARE_START),
    )
  } else {
    beginWork(TimerState(config = config), round = 1)
  }

private fun beginWork(
  state: TimerState,
  round: Int,
): TimerTransition =
  TimerTransition(
    state.copy(
      phase = TimerPhase.WORK,
      round = round,
      phaseMillis = state.config.workMillis,
      remainingMillis = state.config.workMillis,
      running = true,
    ),
    listOf(TimerCue.WORK_START),
  )

private fun beginRest(
  state: TimerState,
  round: Int,
): TimerTransition =
  TimerTransition(
    state.copy(
      phase = TimerPhase.REST,
      round = round,
      phaseMillis = state.config.restMillis,
      remainingMillis = state.config.restMillis,
      running = true,
    ),
  )

private fun advancePhase(state: TimerState): TimerTransition =
  when (state.phase) {
    TimerPhase.PREPARE -> beginWork(state, round = 1)
    TimerPhase.WORK -> advanceAfterWork(state)
    TimerPhase.REST -> beginWork(state, round = state.round + 1)
    TimerPhase.IDLE, TimerPhase.FINISHED -> TimerTransition(state)
  }

private fun advanceAfterWork(state: TimerState): TimerTransition =
  when {
    state.round >= state.config.rounds ->
      TimerTransition(
        state.copy(phase = TimerPhase.FINISHED, remainingMillis = 0L, phaseMillis = 0L, running = false),
        listOf(TimerCue.FINISHED),
      )
    state.config.restMillis > 0L -> {
      val rest = beginRest(state, state.round)
      TimerTransition(rest.state, listOf(TimerCue.WORK_END) + rest.cues)
    }
    else -> {
      val work = beginWork(state, state.round + 1)
      TimerTransition(work.state, listOf(TimerCue.WORK_END) + work.cues)
    }
  }

private fun cuesBetween(
  state: TimerState,
  beforeMillis: Long,
  afterMillis: Long,
): List<TimerCue> = halfWayCues(state, beforeMillis, afterMillis) + countdownCues(state, beforeMillis, afterMillis)

private fun halfWayCues(
  state: TimerState,
  beforeMillis: Long,
  afterMillis: Long,
): List<TimerCue> {
  if (state.phase != TimerPhase.WORK || !state.config.halfWayEnabled) return emptyList()
  val half = state.phaseMillis / 2L
  val crossed = half > 0L && beforeMillis > half && afterMillis <= half
  return if (crossed) listOf(TimerCue.HALF_WAY) else emptyList()
}

// A 3-2-1 countdown runs at the end of every phase that leads into (or ends)
// a work round: prepare -> first round, rest -> next round, and the round's
// own final seconds.
private fun countdownCues(
  state: TimerState,
  beforeMillis: Long,
  afterMillis: Long,
): List<TimerCue> {
  val config = state.config
  val active = state.phase.isActivePhase() && config.countdownEnabled && config.countdownSeconds > 0
  val limit = config.countdownSeconds * MILLIS_PER_SECOND
  if (!active || afterMillis !in 1L..limit) return emptyList()

  // Emit one cue for each whole-second the displayed countdown moves into
  // (e.g. it becomes 3, then 2, then 1 second remaining).
  val firstSecond = minOf(ceilToSecond(beforeMillis) - 1L, config.countdownSeconds.toLong())
  val lastSecond = maxOf(ceilToSecond(afterMillis), 1L)
  val count = (firstSecond - lastSecond + 1L).coerceAtLeast(0L).toInt()
  return List(count) { TimerCue.COUNTDOWN }
}

private fun ceilToSecond(millis: Long): Long = (millis + CEIL_ROUNDING_OFFSET) / MILLIS_PER_SECOND
