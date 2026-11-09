package com.alieducational.app.util

interface Clock {
    fun nowMillis(): Long
}

object SystemClockImpl : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
