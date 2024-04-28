package net.craftventure.core.feature.kart.physicscontroller

import net.craftventure.audioserver.spatial.SpatialAudio
import net.craftventure.bukkit.ktx.extension.rotateY
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.animation.curve.LineairPointCurve
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.force
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.physics.ExternalRigidBody
import net.craftventure.core.physics.Vec2
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.LookAtUtil
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CarPhysicsController : PhysicsController() {
    private var motorEffect: SpatialAudio? = null

    private fun initialiseMotorEffect(kart: Kart) {
        if (motorEffect != null) return
        motorEffect = if (kart.properties.engine.motorSoundUrl?.isPresent == true) SpatialAudio(
            kart.location.toLocation(kart.world),
            soundUrl = kart.properties.engine.motorSoundUrl.force,
            distance = 15.0
        ).apply {
            refDistance(1.0)
            maxDistance(5.0)
            playing(true)
            rate(0.5)
            volume(0.0)
            start()
        } else null
    }

    protected fun finalize() {
        motorEffect?.stop()
    }

    override fun create(kart: Kart) {
        super.create(kart)
        initialiseMotorEffect(kart)
    }

    override fun destroy(kart: Kart) {
        super.destroy(kart)
        motorEffect?.stop()
        motorEffect = null
    }

    private var wasDriftingLastUpdate = false

    override fun updatePhysics(kart: Kart, body: ExternalRigidBody, interactor: Kart.PhysicsInteractor) {
        body.mass = kart.properties.handling.mass.force
        val isControlsEnabled = (kart.kartOwner == null || kart.kartOwner.isKartEnabled(kart)) && !kart.isParked()
        val properties = kart.properties
        val controller = kart.controller
        val velocity = kart.velocity

        kart.updateForward(body)
        kart.updateRight(body)

        val forwardForce = forwardVelocity.length()
        val sideForce = rightVelocity.length()

        val isDrivingBackwards =
            if (forwardForce in -0.001..0.001) controller.forward() < 0 else forward.dot(forwardVelocity) < 0
        val isGivingForceInDrivingDirection =
            !isDrivingBackwards && controller.forward() > 0 || isDrivingBackwards && controller.forward() < 0
        val isBraking = controller.forward() != 0f && !isGivingForceInDrivingDirection
        val isManuallyHandbraking = (isControlsEnabled && controller.isHandbraking()) || kart.isParked()
        val isHandbraking = isManuallyHandbraking || isBraking

        val realForwardForce = if (isDrivingBackwards) -forwardForce else forwardForce

        val wheelFrictionFactor = getCurrentWheelFrictionFactor(kart, interactor)
        val clampedWheelFrictionFactor = wheelFrictionFactor.clamp(
            properties.tires.minWheelFrictionFactor.force,
            properties.tires.maxWheelFrictionFactor.force
        )
        val wheelGripFactor =
            if (properties.tires.minWheelFrictionFactor == properties.tires.maxWheelFrictionFactor) 1.0
            else 1 - InterpolationUtils.getMu(
                properties.tires.minWheelFrictionFactor.force,
                properties.tires.maxWheelFrictionFactor.force,
                clampedWheelFrictionFactor
            )

        val speedRatio =
            forwardForce / properties.engine.forwardSpeed.force // 1.0 = set max speed for engine (not clamped, so can be higher)

//        val forwardSideRatio = if (sideForce == 0.0) 0.0 else sideForce / (sideForce + forwardForce)
//            val wheelFriction = (2.5 * wheelFrictionFactor).clamp(0.0, 1.0)

        val steerForceCurveForSpeed = kart.properties.steer.forceCurveForSpeed.force
        val oversteerCurveForSpeed = kart.properties.steer.oversteerCurveForSpeed?.get() ?: LineairPointCurve.empty
        val engineForceCurveForSpeed = kart.properties.engine.speedForceCurveForSpeed.force
        val brakeCurveForSpeed = kart.properties.brakes.brakeCurveForSpeed.force

        val shouldBeDrifting =
            sideForce >= (kart.properties.tires.sideForceRequiredForDriftingForSpeed?.get()
                ?: LineairPointCurve.empty).translate(realForwardForce) && (isManuallyHandbraking || wasDriftingLastUpdate) //slipFactor.translate(forwardForce)
        wasDriftingLastUpdate = shouldBeDrifting
        interactor.skidParticlesAreRubber = isHandbraking || shouldBeDrifting//forwardSideRatio >= 0.05
        interactor.showSkidParticles = forwardForce * 20 > 0.1 || sideForce * 20 > 0.1

        velocity.x = 0.0
        velocity.y -= 0.06
        velocity.z = kart.currentSpeed
        velocity.rotateY(-body.angle)

        val sideFactor = when {
            shouldBeDrifting -> (kart.properties.tires.sideForceRecoverWhileDriftingForSpeed?.get()
                ?: LineairPointCurve.empty).translate(realForwardForce)

            !kart.currentlyOnGround -> 1.0
//            isHandbraking -> 0.85
            else -> (kart.properties.tires.sideForceRecoverForSpeed?.get()
                ?: LineairPointCurve.empty).translate(realForwardForce)
        }.clamp(0.0, 1.0)

        body.velocity.set(
            forwardVelocity.x + (rightVelocity.x * sideFactor),
            forwardVelocity.z + (rightVelocity.z * sideFactor)
        )

        kart.currentSpeed = if (realForwardForce >= 0) body.velocity.size else -body.velocity.size

        body.linearDamping = 0.0
        if (kart.currentlyOnGround) {
            body.linearDamping += 0.15 * (forwardForce + sideForce)
            body.linearDamping += forwardForce * wheelFrictionFactor * wheelGripFactor
//            body.linearDamping += 0.4 - (wheelGripFactor * 0.3) + (0.15 * speedRatio)
        }
        body.linearDamping = body.linearDamping.coerceAtLeast(0.10)

        if (kart.currentlyOnGround) {
            val forceMultiplier =
                if (!isControlsEnabled || (isHandbraking && !isGivingForceInDrivingDirection)) 0.0 else controller.forward() * (/*if (isGivingForceInDrivingDirection) */engineForceCurveForSpeed.translate(
                    realForwardForce
                ))

            val guessedNextSpeed = forwardForce * body.currentVelocityScale
            val maxForceIncrease =
                (if (controller.forward() >= 0) kart.properties.engine.forwardSpeed.force else kart.properties.engine.backwardSpeed.force) - guessedNextSpeed
            val maxForceSize = maxForceIncrease / body.forceScale

            if (maxForceSize > 0.0) {
                val force =
                    Vector().set(forward).normalize().multiply(forceMultiplier.clamp(-maxForceSize, maxForceSize))

                body.applyForce(
                    Vec2(
                        force.x,
                        force.z
                    ),
                    Vec2(
                        forward.x * (kart.properties.handling.forceOffset.orElse() ?: 0.0),
                        forward.z * (kart.properties.handling.forceOffset.orElse() ?: 0.0)
                    )
                )
            }
        }

        if (isHandbraking) {
            if (forwardForce in -0.001..0.001 && sideForce in -0.001..0.001) {
                body.velocity.set(0.0, 0.0)
            } else {
                val brakeForce = brakeCurveForSpeed.translate(realForwardForce)
                val appliedBrakeForce = if (realForwardForce < 0)
                    max(brakeForce, realForwardForce / body.forceScale)
                else
                    min(brakeForce, realForwardForce / body.forceScale)

                body.applyForce(
                    Vec2(
                        forward.x * -appliedBrakeForce,
                        forward.z * -appliedBrakeForce
                    ),
                    Vec2(0.0, 0.0)
                )
            }
        }

        val airSteerInfluence = kart.properties.handling.airSteerInfluence.force//

        val steerForce = Math.toRadians(-controller.sideways() * steerForceCurveForSpeed.translate(realForwardForce))
        val driftSteerForce = if (shouldBeDrifting)
            Math.toRadians(
                -controller.sideways() * oversteerCurveForSpeed.translate(
                    realForwardForce
                )
            ) else 0.0
        body.angle += (steerForce + driftSteerForce) *
                (if (kart.currentlyOnGround) 1.0 else airSteerInfluence) *
                realForwardForce
        body.update()

        interactor.speedInBpt = forwardForce

        motorEffect?.runInBatch {
            setOrientation(Math.toDegrees(body.angle), 0.0)
            setLocation(kart.location)
            val kmh = abs(CoasterMathUtils.bptToKmh(forwardForce))
//        Logger.debug(currentSpeed.toString())
            volume((0.1 + (speedRatio)).clamp(0.0, properties.engine.motorVolume.force))
            rate(
                (properties.engine.motorBaseRate.force + speedRatio).clamp(
                    properties.engine.motorMinRate.force,
                    properties.engine.motorMaxRate.force
                )
            )
        }
    }

    override fun afterMatrix(
        kart: Kart,
        body: ExternalRigidBody,
        interactor: Kart.PhysicsInteractor,
        matrix4x4: Matrix4x4
    ) {
        super.afterMatrix(kart, body, interactor, matrix4x4)
        if (true) return

        val speed = interactor.speedInBpt
        if (speed != 0.0) {
            val yawPitch = LookAtUtil.YawPitch()
            LookAtUtil.getYawPitchFromRadian(kart.lastLocation, kart.location, yawPitch)
            val pitch = if (kart.lastLocation == kart.location) 0.0 else Math.toDegrees(yawPitch.pitch) + 90.0

            val forwardForce = forwardVelocity.length()
            val controller = kart.controller
            val isDrivingBackwards =
                if (forwardForce in -0.001..0.001) controller.forward() < 0 else forward.dot(forwardVelocity) < 0

            matrix4x4.rotateYawPitchRoll(if (isDrivingBackwards) pitch else -pitch, 0.0, 0.0)
        }
    }
}