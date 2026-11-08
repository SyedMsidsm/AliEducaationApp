package com.alieducational.app.auth

import com.alieducational.app.data.preferences.PinThrottlePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinThrottleUnitTest {

    @Test
    fun testFiveFailuresTriggersOneMinuteLockout_SEC04() {
        var state = PinThrottlePolicy.State()
        var now = 1_000_000L

        // 4 failed attempts should not trigger block
        for (i in 1..4) {
            val (newState, success) = PinThrottlePolicy.onAttempt(state, isValid = false, now = now)
            assertFalse("Attempt $i should fail", success)
            assertEquals("Attempt count should be $i", i, newState.failedAttempts)
            assertEquals(0L, newState.blockedUntil)
            state = newState
        }

        // 5th failed attempt triggers 1-minute lockout (60,000 ms)
        val (blockedState, success) = PinThrottlePolicy.onAttempt(state, isValid = false, now = now)
        assertFalse("5th attempt should fail", success)
        assertEquals("Attempts should reset to 0 after block trigger", 0, blockedState.failedAttempts)
        assertEquals("Failure round should advance to 1", 1, blockedState.failureRounds)
        assertEquals("Should be blocked for exactly 60,000 ms", now + 60_000L, blockedState.blockedUntil)
        assertTrue("Should report as blocked", PinThrottlePolicy.isBlocked(blockedState, now))
    }

    @Test
    fun testWrongPinDuringBlockStaysBlocked_SEC04() {
        var state = PinThrottlePolicy.State(failedAttempts = 0, failureRounds = 1, blockedUntil = 1_060_000L)
        val now = 1_030_000L // 30s into 60s block

        // Attempt during block must fail and NOT alter the block timestamp
        val (stateDuringBlock, success) = PinThrottlePolicy.onAttempt(state, isValid = false, now = now)
        assertFalse("Attempt during block must fail", success)
        assertEquals("Block expiry must not change on attempt during block", 1_060_000L, stateDuringBlock.blockedUntil)
        assertEquals("Rounds must not advance while blocked", 1, stateDuringBlock.failureRounds)
        assertEquals("Failed attempts must not advance while blocked", 0, stateDuringBlock.failedAttempts)
    }

    @Test
    fun testCorrectPinAfterBlockExpiresSucceedsAndResetsRounds_SEC04() {
        var state = PinThrottlePolicy.State(failedAttempts = 0, failureRounds = 2, blockedUntil = 1_060_000L)
        val afterExpiry = 1_060_001L

        assertFalse("Should no longer be blocked after expiry", PinThrottlePolicy.isBlocked(state, afterExpiry))

        val (successState, success) = PinThrottlePolicy.onAttempt(state, isValid = true, now = afterExpiry)
        assertTrue("Correct PIN after expiry must succeed", success)
        assertEquals("Failed attempts must be reset to 0", 0, successState.failedAttempts)
        assertEquals("Failure rounds must be reset to 0", 0, successState.failureRounds)
        assertEquals("Blocked until must be reset to 0", 0L, successState.blockedUntil)
    }

    @Test
    fun testEscalatingLockoutSchedule_SEC04() {
        // Schedule: Round 0 -> 1 min, Round 1 -> 5 min, Round 2 -> 15 min, Round 3+ -> 60 min (cap)
        assertEquals(60_000L, PinThrottlePolicy.lockoutDurationMs(0))     // 1 min
        assertEquals(300_000L, PinThrottlePolicy.lockoutDurationMs(1))    // 5 min
        assertEquals(900_000L, PinThrottlePolicy.lockoutDurationMs(2))    // 15 min
        assertEquals(3_600_000L, PinThrottlePolicy.lockoutDurationMs(3))  // 60 min
        assertEquals(3_600_000L, PinThrottlePolicy.lockoutDurationMs(4))  // 60 min (capped)
        assertEquals(3_600_000L, PinThrottlePolicy.lockoutDurationMs(10)) // 60 min (capped)
    }

    @Test
    fun testFullEscalationFlow_SEC04() {
        var state = PinThrottlePolicy.State()
        var now = 10_000_000L

        // Round 1: 5 fails -> 1 min
        repeat(5) {
            state = PinThrottlePolicy.onAttempt(state, isValid = false, now = now).first
        }
        assertEquals(now + 60_000L, state.blockedUntil)
        assertEquals(1, state.failureRounds)

        // Advance past 1 min block
        now += 60_001L

        // Round 2: 5 fails -> 5 min (300,000 ms)
        repeat(5) {
            state = PinThrottlePolicy.onAttempt(state, isValid = false, now = now).first
        }
        assertEquals(now + 300_000L, state.blockedUntil)
        assertEquals(2, state.failureRounds)

        // Advance past 5 min block
        now += 300_001L

        // Round 3: 5 fails -> 15 min (900,000 ms)
        repeat(5) {
            state = PinThrottlePolicy.onAttempt(state, isValid = false, now = now).first
        }
        assertEquals(now + 900_000L, state.blockedUntil)
        assertEquals(3, state.failureRounds)

        // Advance past 15 min block
        now += 900_001L

        // Round 4: 5 fails -> 60 min (3,600,000 ms cap)
        repeat(5) {
            state = PinThrottlePolicy.onAttempt(state, isValid = false, now = now).first
        }
        assertEquals(now + 3_600_000L, state.blockedUntil)
        assertEquals(4, state.failureRounds)

        // Advance past 60 min block and enter correct PIN -> all reset
        now += 3_600_001L
        val (resetState, success) = PinThrottlePolicy.onAttempt(state, isValid = true, now = now)
        assertTrue(success)
        assertEquals(0, resetState.failedAttempts)
        assertEquals(0, resetState.failureRounds)
        assertEquals(0L, resetState.blockedUntil)
    }
}
