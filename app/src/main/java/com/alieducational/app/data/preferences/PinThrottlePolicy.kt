package com.alieducational.app.data.preferences

/**
 * Pure policy implementation for escalating Owner PIN lockout (Section SEC-04).
 *
 * Escalating schedule:
 * - Round 0: 5 consecutive failures -> 1 min lockout (60,000 ms)
 * - Round 1: 5 consecutive failures -> 5 min lockout (300,000 ms)
 * - Round 2: 5 consecutive failures -> 15 min lockout (900,000 ms)
 * - Round 3+: 5 consecutive failures -> 60 min lockout (3,600,000 ms, capped)
 *
 * Reset: A successful PIN authentication resets failed attempts, failure rounds, and blocked status.
 */
object PinThrottlePolicy {

    fun lockoutDurationMs(round: Int): Long = when (round) {
        0 -> 60_000L       // 1 min
        1 -> 300_000L      // 5 min
        2 -> 900_000L      // 15 min
        else -> 3_600_000L // 60 min (cap)
    }

    data class State(
        val failedAttempts: Int = 0,
        val failureRounds: Int = 0,
        val blockedUntil: Long = 0L
    )

    fun isBlocked(state: State, now: Long): Boolean {
        if (state.blockedUntil <= 0L) return false
        return now < state.blockedUntil
    }

    fun onAttempt(
        state: State,
        isValid: Boolean,
        now: Long
    ): Pair<State, Boolean> {
        // While currently locked out, reject immediately and preserve existing lockout
        if (isBlocked(state, now)) {
            return Pair(state, false)
        }

        if (isValid) {
            // Success resets all failure tracking and unlocks role
            return Pair(State(failedAttempts = 0, failureRounds = 0, blockedUntil = 0L), true)
        }

        // Failed attempt: increment failed attempts
        val newAttempts = state.failedAttempts + 1
        return if (newAttempts >= 5) {
            // 5 consecutive failures trigger escalating lockout
            val duration = lockoutDurationMs(state.failureRounds)
            val nextRounds = state.failureRounds + 1
            Pair(
                State(
                    failedAttempts = 0,
                    failureRounds = nextRounds,
                    blockedUntil = now + duration
                ),
                false
            )
        } else {
            Pair(state.copy(failedAttempts = newAttempts), false)
        }
    }
}
