package com.alieducational.app.util

import android.os.SystemClock
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepUpAuth @Inject constructor(
    private val bio: BiometricAuthManager
) {
    private var clock: () -> Long = { SystemClock.elapsedRealtime() }

    // Secondary constructor for unit testing with fake clock
    constructor(bio: BiometricAuthManager, clock: () -> Long) : this(bio) {
        this.clock = clock
    }

    private var lastOk = 0L

    fun reset() {
        lastOk = 0L
    }

    private fun fresh(windowMs: Long) = lastOk != 0L && (clock() - lastOk) < windowMs

    fun require(
        activity: FragmentActivity,
        title: String,
        windowMs: Long = 60_000L,
        onOk: () -> Unit,
        onFail: (String) -> Unit
    ) {
        if (fresh(windowMs)) {
            onOk()
            return
        }
        if (!bio.canAuthenticate()) {
            onFail("Set up a screen lock first")
            return
        }
        bio.promptBiometric(
            activity = activity,
            title = title,
            onSuccess = {
                lastOk = clock()
                onOk()
            },
            onError = onFail
        )
    }
}
