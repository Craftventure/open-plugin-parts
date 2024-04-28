package net.craftventure.core.ride

import net.craftventure.bukkit.ktx.extension.set
import org.bukkit.util.Vector

class TeleportInterpolationFixer(
    val steps: Int = 3
) {
    private val currentPosition = Vector()
    private val deltaPosition = Vector()
    private val nextPosition = Vector()
    private val calculatedPosition = Vector()

    private var firstSet = true
    private val interpolation = 1 / steps.toDouble()

    fun setNextLocation(location: Vector) = setNextLocation(location.x, location.y, location.z)

    fun setNextLocation(x: Double, y: Double, z: Double) {
        if (firstSet) {
            currentPosition.set(x, y, z)
            calculatedPosition.set(currentPosition)
            firstSet = false
        } else {
            currentPosition.set(calculatedPosition)
        }
        nextPosition.set(x, y, z)

        val deltaX = (nextPosition.x - calculatedPosition.x) * interpolation
        val deltaY = (nextPosition.y - calculatedPosition.y) * interpolation
        val deltaZ = (nextPosition.z - calculatedPosition.z) * interpolation

        deltaPosition.set(deltaX, deltaY, deltaZ)

        calculatedPosition.set(
            currentPosition.x + deltaPosition.x,
            currentPosition.y + deltaPosition.y,
            currentPosition.z + deltaPosition.z
        )
    }

    fun getCurrentPosition(): Vector {
        return calculatedPosition
    }
}