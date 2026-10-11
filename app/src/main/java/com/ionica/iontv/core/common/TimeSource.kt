package com.ionica.iontv.core.common

import javax.inject.Inject
import javax.inject.Singleton

fun interface TimeSource {
    fun nowMillis(): Long
}

@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
