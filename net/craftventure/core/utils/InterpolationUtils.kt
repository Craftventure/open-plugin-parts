package net.craftventure.core.utils

object InterpolationUtils {
    @JvmStatic
    fun linearInterpolate(y1: Double, y2: Double, mu: Double): Double {
        return if (y1 == y2) y1 else y1 * (1 - mu) + y2 * mu
    }

    /**
     * @return any double that is NOT clamped
     */
    fun getMu(from: Double, to: Double, actualValue: Double): Double = (actualValue - from) / (to - from)


}