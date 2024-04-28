package net.craftventure.core.physics

import net.craftventure.core.ktx.extension.clamp
import kotlin.math.pow


class ExternalRigidBody {
    var mass: Double = 140.0
    var inertia: Double = 5.0
    var linearDamping = 0.68 /* asphalt */ - 0.15 /* air */
        set(value) {
            field = value.clamp(0.0, 1.0)
        }
    var additionalLinearDamping = 0.0

    var angularDamping = 0.90
        set(value) {
            field = value.clamp(0.0, 1.0)
        }

    val velocity = Vec2()
    val acceleration = Vec2()
    val force = Vec2()

    var angle = 0.0
    var angularVelocity = 0.0
    var angularAcceleration = 0.0
    var torque = 0.0

    val forceScale: Double
        get() = 1.0 / mass

    val currentVelocityScale: Double
        get() = (1.0 - (linearDamping + additionalLinearDamping).clamp(0.0, 1.0)).pow(0.05)

    val vTmp = Vec2()

    fun applyForce(f: Vec2, relativePosition: Vec2) {
        this.force.add(f)
        vTmp.set(relativePosition)
        applyTorque(vTmp.cross(f))
    }

    fun applyTorque(torque: Double) {
        this.torque += torque
    }

    fun applyImpulse(impulse: Vec2, relativePosition: Vec2) {
        vTmp.set(impulse)
        vTmp.scale(1.0 / mass)
        velocity.add(vTmp)

        vTmp.set(relativePosition)
        angularVelocity += vTmp.cross(impulse) / inertia
    }

    fun update() {
        force.scale(1.0 / mass)
        acceleration.set(force)

        velocity.scale(currentVelocityScale)
//        Logger.debug("velocity=${angularVelocity.format(2)} ${((1.0 - angularDamping).pow(0.05)).format(2)}")
        angularVelocity *= (1.0 - angularDamping).pow(0.05)

        velocity.add(acceleration)

        angle += angularVelocity
        angularVelocity += angularAcceleration
        angularAcceleration = if (inertia != 0.0) torque / inertia else 0.0

        force.set(0.0, 0.0)
        torque = 0.0
        additionalLinearDamping = 0.0
    }
}