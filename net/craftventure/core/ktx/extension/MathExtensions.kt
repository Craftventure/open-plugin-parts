package net.craftventure.core.ktx.extension


fun Double.clamp(min: Double, max: Double): Double = Math.max(min, Math.min(this, max))
fun Float.clamp(min: Float, max: Float): Float = Math.max(min, Math.min(this, max))
fun Long.clamp(min: Double, max: Double): Double = Math.max(min, Math.min(this.toDouble(), max))
fun Int.clamp(min: Int, max: Int): Int = if (this > max) max else if (this < min) min else this

fun Double.clampDegrees(): Double {
    var value = this
    while (value < 0) {
        value += 360
    }
    return value % 360
}