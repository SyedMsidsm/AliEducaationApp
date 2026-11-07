package com.alieducational.inventory

/** Monotonic time only: wall-clock or date changes cannot extend an unlocked session. */
class SessionPolicy {
    var unlocked = false
        private set
    var timeoutMs = 60_000L
        set(value) { require(value in setOf(30_000L,60_000L,300_000L)); field=value }
    private var lastActive = 0L
    fun unlock(now: Long) { lastActive=now; unlocked=true }
    fun expired(now: Long) = unlocked && (now < lastActive || now-lastActive >= timeoutMs)
    fun touch(now: Long) {
        if (expired(now)) lock()
        if (unlocked) lastActive=now
    }
    fun lock() { unlocked=false }
}
