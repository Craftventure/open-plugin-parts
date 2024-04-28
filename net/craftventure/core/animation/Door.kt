package net.craftventure.core.animation

import java.time.Duration

interface Door {
    fun open(duration: Duration) = open(duration.toMillis().toInt() / 50)
    fun open(ticks: Int)

    fun close(duration: Duration) = close(duration.toMillis().toInt() / 50)
    fun close(ticks: Int)

    fun open(open: Boolean, duration: Duration) = if (open) open(duration) else close(duration)

    fun open(open: Boolean, duration: Int) = if (open) open(duration) else close(duration)
}