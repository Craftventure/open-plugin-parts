package net.craftventure.core.ride.trackedride

object CoasterMathUtils {

    private const val TICKPART = 1.0 / 20.0

    @JvmStatic
    fun kmhToBpt(kmh: Double): Double {
        return kmh / 3.6 * TICKPART
    }

    @JvmStatic
    fun bptToKmh(bpt: Double): Double {
        return bpt / TICKPART * 3.6
    }

    fun btpToMps(bpt: Double): Double {
        return bpt / TICKPART
    }
}