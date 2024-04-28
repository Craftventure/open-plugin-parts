package net.craftventure.core.ktx.extension

import java.util.*

fun Number.format(digits: Int) = java.lang.String.format(Locale.US, "%.${digits}f", this.toDouble())
fun Double.orIfNan(other: Double): Double {
    if (isNaN()) {
        return other
    }
    return this
}

fun Byte.asBoolean() = this != 0.toByte()

val Double.positiveValue: Double
    get() = if (this < 0) -this else this

val Double.isEffectivelyZeroBy4Decimals: Boolean
    get() = this in -0.0001..0.0001

fun Double?.equalsWithPrecision(other: Double?, precision: Double = 0.01): Boolean {
    if (this == null && other == null) return true
    if (this == null && other != null) return false
    if (this != null && other == null) return false
    if (this == null || other == null) return false

    return this < other + precision && other - precision < this
}