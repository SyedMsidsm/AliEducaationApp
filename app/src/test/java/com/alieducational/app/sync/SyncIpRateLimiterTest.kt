package com.alieducational.app.sync

import com.alieducational.app.data.service.SyncIpRateLimiter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncIpRateLimiterTest {

    private lateinit var limiter: SyncIpRateLimiter

    @Before
    fun setUp() {
        limiter = SyncIpRateLimiter(
            maxBadAttempts = 5,
            windowMs = 10 * 60 * 1000L,       // 10 minutes
            blockDurationMs = 15 * 60 * 1000L  // 15 minutes
        )
    }

    @Test
    fun testUnderThresholdNotBlocked() {
        val ip = "192.168.1.50"
        val t0 = 1000000L

        repeat(4) { i ->
            assertFalse(limiter.recordBadAttempt(ip, t0 + i * 1000L))
            assertFalse(limiter.isBlocked(ip, t0 + i * 1000L))
        }
    }

    @Test
    fun testFifthBadAttemptBlocksForFifteenMinutes() {
        val ip = "192.168.1.50"
        val t0 = 1000000L

        repeat(4) { i ->
            limiter.recordBadAttempt(ip, t0 + i * 1000L)
        }
        val fifthTime = t0 + 4000L
        val blocked = limiter.recordBadAttempt(ip, fifthTime)
        assertTrue("Fifth attempt must trigger block", blocked)
        assertTrue("IP must now be blocked", limiter.isBlocked(ip, fifthTime))

        // Check 14 minutes later: still blocked
        val fourteenMinutesLater = fifthTime + 14 * 60 * 1000L
        assertTrue("Still blocked at 14 minutes", limiter.isBlocked(ip, fourteenMinutesLater))

        // Check 15 minutes + 1 second later: unblocked
        val fifteenMinutesLater = fifthTime + 15 * 60 * 1000L + 1000L
        assertFalse("Unblocked after block duration", limiter.isBlocked(ip, fifteenMinutesLater))
    }

    @Test
    fun testAttemptsOutsideWindowDoNotAccumulate() {
        val ip = "192.168.1.50"
        var t = 1000000L

        // 4 attempts spread 15 minutes apart each
        repeat(4) {
            limiter.recordBadAttempt(ip, t)
            t += 15 * 60 * 1000L
            assertFalse("Old attempt should have expired", limiter.isBlocked(ip, t))
        }
        // Even with a 5th attempt after all those spaces, only 1 attempt is in the window
        assertFalse(limiter.recordBadAttempt(ip, t))
        assertFalse(limiter.isBlocked(ip, t))
    }

    @Test
    fun testSuccessClearsBadAttempts() {
        val ip = "192.168.1.50"
        val t0 = 1000000L
        repeat(4) { i ->
            limiter.recordBadAttempt(ip, t0 + i * 1000L)
        }
        limiter.recordSuccess(ip)

        // After success, previous bad attempts are wiped
        repeat(4) { i ->
            assertFalse(limiter.recordBadAttempt(ip, t0 + 10000L + i * 1000L))
        }
        assertFalse(limiter.isBlocked(ip, t0 + 20000L))
    }
}
