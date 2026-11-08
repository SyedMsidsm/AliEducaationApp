package com.alieducational.app.data.service

class SyncIpRateLimiter(
    private val maxBadAttempts: Int = 5,
    private val windowMs: Long = 10 * 60 * 1000L,       // 10 minutes
    private val blockDurationMs: Long = 15 * 60 * 1000L  // 15 minutes
) {
    private data class IpRecord(
        val badAttemptTimestamps: MutableList<Long> = mutableListOf(),
        var blockedUntil: Long = 0L
    )

    private val records = mutableMapOf<String, IpRecord>()

    @Synchronized
    fun isBlocked(ip: String, now: Long = System.currentTimeMillis()): Boolean {
        val record = records[ip] ?: return false
        if (record.blockedUntil > now) return true
        if (record.blockedUntil != 0L && record.blockedUntil <= now) {
            // Block expired
            record.blockedUntil = 0L
            record.badAttemptTimestamps.clear()
        }
        return false
    }

    @Synchronized
    fun recordBadAttempt(ip: String, now: Long = System.currentTimeMillis()): Boolean {
        val record = records.getOrPut(ip) { IpRecord() }
        // Clean timestamps older than window
        record.badAttemptTimestamps.removeAll { it < now - windowMs }
        record.badAttemptTimestamps.add(now)
        if (record.badAttemptTimestamps.size >= maxBadAttempts) {
            record.blockedUntil = now + blockDurationMs
            return true // newly blocked
        }
        return false
    }

    @Synchronized
    fun recordSuccess(ip: String) {
        records.remove(ip)
    }

    @Synchronized
    fun reset() {
        records.clear()
    }
}
