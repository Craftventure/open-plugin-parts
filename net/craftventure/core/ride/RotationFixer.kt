package net.craftventure.core.ride

import net.craftventure.core.utils.MathUtil
import org.bukkit.util.EulerAngle

class RotationFixer(
    val steps: Int = 3
) {
    private val currentRotation = Rotation()
    private val deltaRotation = Rotation()
    private val nextRotation = Rotation()
    private val calculatedRotation = Rotation()

    private val currentQuaternion = Quaternion()
    private val nextQuaternion = Quaternion()
    private val calculatedQuaternion = Quaternion()

    private var firstSet = true
    private val interpolation = 1 / steps.toDouble()

    @JvmOverloads
    fun setNextRotation(rotation: Quaternion, debug: Boolean = false) {
        if (firstSet) {
            currentQuaternion.setTo(rotation)
            calculatedQuaternion.setTo(currentQuaternion)
            firstSet = false
        } else {
            currentQuaternion.setTo(calculatedQuaternion)
        }
        nextQuaternion.setTo(rotation)

        calculatedQuaternion.interpolateWith(nextQuaternion, interpolation)
        val pose = TransformUtils.getArmorStandPose(calculatedQuaternion)
        calculatedRotation.set(pose.x, pose.y, pose.z)
    }

    @Deprecated(message = "Use quaternions where possible")
    @JvmOverloads
    fun setNextRotation(x: Double, y: Double, z: Double, debug: Boolean = false) {
        if (firstSet) {
            currentRotation.set(x, y, z)
            calculatedRotation.set(currentRotation)
            firstSet = false
        } else {
            currentRotation.set(calculatedRotation)
        }
        nextRotation.set(x, y, z)

        val deltaX = Math.toDegrees(
            MathUtil.deltaRadian(
                Math.toRadians(nextRotation.x),
                Math.toRadians(calculatedRotation.x)
            ) * interpolation
        )
        val deltaY = Math.toDegrees(
            MathUtil.deltaRadian(
                Math.toRadians(nextRotation.y),
                Math.toRadians(calculatedRotation.y)
            ) * interpolation
        )
        val deltaZ = Math.toDegrees(
            MathUtil.deltaRadian(
                Math.toRadians(nextRotation.z),
                Math.toRadians(calculatedRotation.z)
            ) * interpolation
        )

        deltaRotation.set(deltaX, deltaY, deltaZ)

        calculatedRotation.set(
            currentRotation.x + deltaRotation.x,
            currentRotation.y + deltaRotation.y,
            currentRotation.z + deltaRotation.z
        )
    }

    fun getCurrentQuaternion(): Quaternion {
        return calculatedQuaternion
    }

    fun getCurrentRotation(): Rotation {
        return calculatedRotation
    }

    data class Rotation(
        var x: Double = 0.0,
        var y: Double = 0.0,
        var z: Double = 0.0
    ) {
        fun set(x: Double, y: Double, z: Double) {
            this.x = x
            this.y = y
            this.z = z
        }

        fun set(rotation: Rotation) {
            x = rotation.x
            y = rotation.y
            z = rotation.z
        }

        fun toEuler() = EulerAngle(x, y, z)
    }
}