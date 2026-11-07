package com.alieducational.inventory
import org.junit.Assert.*
import org.junit.Test

class SessionPolicyTest {
    @Test fun locksAtExactMinute() {
        val s=SessionPolicy();s.unlock(1000)
        assertFalse(s.expired(60999));assertTrue(s.expired(61000))
    }
    @Test fun lateTouchCannotReviveSession() {
        val s=SessionPolicy();s.unlock(1000);s.touch(61000)
        assertFalse(s.unlocked)
    }
    @Test fun briefSwitchDoesNotExpireButLongSwitchDoes() {
        val s=SessionPolicy();s.unlock(1000)
        assertFalse(s.expired(11000));assertTrue(s.expired(62000))
    }
    @Test fun activityBeforeDeadlineExtendsSession() {
        val s=SessionPolicy();s.unlock(1000);s.touch(50000)
        assertFalse(s.expired(100000));assertTrue(s.expired(110000))
    }
    @Test fun clockAnomalyFailsClosed() {
        val s=SessionPolicy();s.unlock(5000);assertTrue(s.expired(4000))
    }
    @Test fun manualLockDoesNotAllowTouchToUnlock() {
        val s=SessionPolicy();s.unlock(0);s.lock();s.touch(1000);assertFalse(s.unlocked)
    }
}
