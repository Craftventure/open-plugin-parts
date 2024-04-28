package net.craftventure.core.ktx.util

object AngleUtils {
    /**
     * Shortest distance (angular) between two angles.
     * It will be in range [-180, 180].
     */
    fun distance(angle1: Double, angle2: Double): Double {
        val diff = (angle2 - angle1 + 180) % 360 - 180
        return if (diff < -180) diff + 360 else diff
    }

    fun smallestMoveTo(inputDegrees: Double, targetDegrees: Double, maxChange: Double): Double {
        val distance = distance(inputDegrees, targetDegrees)
        return if (distance >= 0) {
            if (distance > maxChange) {
                inputDegrees + maxChange
            } else {
                inputDegrees + distance
            }
        } else {
            if (distance < -maxChange) {
                inputDegrees + -maxChange
            } else {
                inputDegrees + distance
            }
        }
    }

    fun clampDegrees(degree: Double): Double {
        var output = degree
        while (output < 0)
            output += 360
        return output % 360
    }
}