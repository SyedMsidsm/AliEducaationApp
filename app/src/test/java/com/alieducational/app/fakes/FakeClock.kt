package com.alieducational.app.fakes

import com.alieducational.app.util.Clock

class FakeClock(private var current: Long = 0L) : Clock {
    override fun nowMillis(): Long = current

    fun set(millis: Long) {
        current = millis
    }

    fun advance(millis: Long) {
        current += millis
    }
}
