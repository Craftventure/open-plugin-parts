package net.craftventure.core.ktx.extension

import java.util.*

fun IntRange.t(value: Number): Double {
    return ((value.toLong() - this.first).toDouble() / (this.last - this.first).toDouble()).clamp(0.0, 1.0)
}

fun ClosedRange<Int>.random() = Random().nextInt((endInclusive + 1) - start) + start