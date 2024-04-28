package net.craftventure.core.utils

import java.util.concurrent.TimeUnit

data class TimeSpan(val value: Long, val timeUnit: TimeUnit) {
    fun toMillis() = timeUnit.toMillis(value)
    fun toSeconds() = timeUnit.toSeconds(value)
}