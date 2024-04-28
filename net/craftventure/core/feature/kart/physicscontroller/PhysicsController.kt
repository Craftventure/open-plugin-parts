package net.craftventure.core.feature.kart.physicscontroller

import net.craftventure.bukkit.ktx.extension.getFrictionFactor
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.physics.ExternalRigidBody
import org.bukkit.util.Vector

abstract class PhysicsController {
    protected var kart: Kart? = null
    protected val forward = Vector()
    protected val forwardVelocity = Vector()
    protected val right = Vector()
    protected val rightVelocity = Vector()

    protected fun Kart.updateForward(body: ExternalRigidBody) {
        forward.set(0.0, 0.0, 1.0)
            .rotateAroundY(-body.angle)

        forwardVelocity.set(
            forward.clone().multiply(
                Vector().set(body.velocity.x, velocity.y, body.velocity.y).dot(
                    forward
                )
            )
        )
    }

    protected fun Kart.updateRight(body: ExternalRigidBody) {
        right.set(1.0, 0.0, 0.0)
            .rotateAroundY(-body.angle)

        rightVelocity.set(right.clone().multiply(Vector().set(body.velocity.x, velocity.y, body.velocity.y).dot(right)))
    }

    protected fun getCurrentWheelFrictionFactor(kart: Kart, interactor: Kart.PhysicsInteractor): Double {
        kart.apply {
            var averageWheelFrictionFactor = 0.0
            val fallbackFriction = kart.properties.handling.fallbackFriction?.orElse()

            for (wheel in wheels) {
                val wheelPosition = wheel.currentMatrix.toVector()
                val block = world!!.getBlockAt(
                    wheelPosition.x.toInt(),
                    (wheelPosition.y - 0.05 - (if (wheel.config.diameter != null) wheel.config.diameter!! * 0.5 else 0.0)).toInt(),
                    wheelPosition.z.toInt()
                )
                val friction = block.getFrictionFactor()
                if (friction != null) {
                    averageWheelFrictionFactor += friction
                } else if (fallbackFriction != null) {
                    averageWheelFrictionFactor = fallbackFriction
                }
            }
            if (averageWheelFrictionFactor == 0.0) return averageWheelFrictionFactor
            return averageWheelFrictionFactor / wheels.size.toDouble()
        }
    }

    abstract fun updatePhysics(kart: Kart, body: ExternalRigidBody, interactor: Kart.PhysicsInteractor)

    open fun afterMatrix(
        kart: Kart,
        body: ExternalRigidBody,
        interactor: Kart.PhysicsInteractor,
        matrix4x4: Matrix4x4
    ) {
    }

    open fun create(kart: Kart) {
        if (this.kart !== null) throw IllegalStateException("PhysicsController already attached to another kart")
        this.kart = kart
    }

    open fun destroy(kart: Kart) {
        this.kart = null
    }

    companion object {
        val REGISTER = hashMapOf<String, Class<out PhysicsController>>()

        fun reverseRegister(controller: PhysicsController) = REGISTER.entries
            .firstOrNull { it.value.isInstance(controller) }?.key

        init {
            REGISTER["car"] = CarPhysicsController::class.java
            REGISTER["zeppelin"] = ZeppelinPhysicsController::class.java
            REGISTER["plane"] = PlanePhysicsController::class.java
        }
    }
}