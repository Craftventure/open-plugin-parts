package net.craftventure.core.feature.kart.physicscontroller

import net.craftventure.audioserver.spatial.SpatialAudio
import net.craftventure.bukkit.ktx.extension.rotateY
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.animation.curve.LineairPointCurve
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.force
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.physics.ExternalRigidBody
import net.craftventure.core.physics.Vec2
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.LookAtUtil
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlanePhysicsController : PhysicsController() {
    private val motorEffect: SpatialAudio? by lazy {
        val kart = this.kart!!
        if (kart.properties.engine.motorSoundUrl != null) SpatialAudio(
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

    override fun destroy(kart: Kart) {
        super.destroy(kart)
        motorEffect?.stop()
    }

    override fun updatePhysics(kart: Kart, body: ExternalRigidBody, interactor: Kart.PhysicsInteractor) {
        body.mass = kart.properties.handling.mass.force
        val isControlsEnabled = (kart.kartOwner == null || kart.kartOwner.isKartEnabled(kart)) && !kart.isParked()
        val properties = kart.properties
        val controller = kart.controller
        val velocity = kart.velocity
        val planeLifter = properties.planeLifter!!

        kart.updateForward(body)
        kart.updateRight(body)

        val currentlyOnGround = kart.currentlyOnGround
        val forwardForce = forwardVelocity.length()
        val sideForce = rightVelocity.length()

        val isDrivingBackwards =
            if (forwardForce in -0.001..0.001) controller.forward() < 0 else forward.dot(forwardVelocity) < 0
        val isGivingForceInDrivingDirection =
            !isDrivingBackwards && controller.forward() > 0 || isDrivingBackwards && controller.forward() < 0
        val isBraking = controller.forward() != 0f && !isGivingForceInDrivingDirection
        val isManuallyHandbraking = kart.isParked()
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
//        val oversteerCurveForSpeed = kart.properties.steer.oversteerCurveForSpeed?.get() ?: LineairPointCurve.empty
        val engineForceCurveForSpeed = kart.properties.engine.speedForceCurveForSpeed.force
        val brakeCurveForSpeed = kart.properties.brakes.brakeCurveForSpeed.force

        val isFlyingAtLiftSpeed = forwardForce >= planeLifter.minimumFlySpeed
        if (isControlsEnabled) {
            if (isFlyingAtLiftSpeed && (controller.isHandbraking() || kart.isParked())) {
                val limit = planeLifter.flyVelocityUpSpeedLimit.force
                if (velocity.y < limit) {
                    velocity.y += planeLifter.flyVelocityUpSpeed.force.translate(kart.currentSpeed)
                    velocity.y = velocity.y.coerceIn(-limit, limit)
                }
            }
            if (controller.isDismounting()) {
                val limit = planeLifter.flyVelocityDownSpeedLimit.force
                if (velocity.y > -limit) {
                    velocity.y -= planeLifter.flyVelocityDownSpeed.force.translate(kart.currentSpeed)
                    velocity.y = velocity.y.coerceIn(-limit, limit)
                }
            }
        }

        if (!isFlyingAtLiftSpeed || kart.isParked()) {
            val limit = planeLifter.maxStallVelocityLimit.force
            if (velocity.y > -limit) {
                velocity.y -= planeLifter.stallSpeedVelocity.force.translate(kart.currentSpeed)
                velocity.y = velocity.y.coerceIn(-limit, limit)
            }
        }

        if (isFlyingAtLiftSpeed || velocity.y >= 0)
            velocity.y *= planeLifter.velocityTickDampening.force.coerceIn(0.0, 1.0)
        if (velocity.y < 0.005 && velocity.y > -0.005) {
            velocity.y = 0.0
        }

        velocity.x = 0.0
        velocity.z = kart.currentSpeed
        velocity.rotateY(-body.angle)

        val sideFactor = when {
//            isHandbraking -> 0.85
            else -> (kart.properties.tires.sideForceRecoverForSpeed?.get()
                ?: LineairPointCurve.empty).translate(realForwardForce)
        }.clamp(0.0, 1.0)

        var forwardVelocityMulitplier = 1.0

//        if (!currentlyOnGround) {
//            val minSpeed = planeLifter.minimumFlySpeed
//            if (forwardForce < minSpeed) {
////                logcat { "Min speed, increasing" }
//                val diff = minSpeed / forwardForce
//                forwardVelocityMulitplier = diff
//            }
//        }

        body.velocity.set(
            (forwardVelocity.x * forwardVelocityMulitplier) + (rightVelocity.x * sideFactor),
            (forwardVelocity.z * forwardVelocityMulitplier) + (rightVelocity.z * sideFactor)
        )

        kart.currentSpeed = if (realForwardForce >= 0) body.velocity.size else -body.velocity.size

        body.linearDamping = 0.0
        if (currentlyOnGround && kart.isParked()) {
            body.linearDamping += 0.15 * (forwardForce + sideForce)
            body.linearDamping += forwardForce * wheelFrictionFactor * wheelGripFactor
//            body.linearDamping += 0.4 - (wheelGripFactor * 0.3) + (0.15 * speedRatio)
            body.linearDamping = body.linearDamping.coerceAtLeast(0.10)
        } else {
            body.linearDamping = body.linearDamping.coerceAtLeast(0.05)
        }

        val forceMultiplier =
            if (!isControlsEnabled || (isHandbraking && !isGivingForceInDrivingDirection)) 0.0 else controller.forward() * (/*if (isGivingForceInDrivingDirection) */engineForceCurveForSpeed.translate(
                realForwardForce
            ))

        val guessedNextSpeed = forwardForce * body.currentVelocityScale
        val maxForceIncrease =
            (if (controller.forward() >= 0) kart.properties.engine.forwardSpeed.force else kart.properties.engine.backwardSpeed.force) - guessedNextSpeed
        val maxForceSize = maxForceIncrease / body.forceScale

        if (maxForceSize > 0.0 && (currentlyOnGround || !isDrivingBackwards)) {
            val force = Vector().set(forward).normalize().multiply(forceMultiplier.clamp(-maxForceSize, maxForceSize))
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
//        if (planeLifter != null && !currentlyOnGround) {
//            val minSpeed = planeLifter.minimumFlySpeed
//            if (kart.currentSpeed < minSpeed) {
//                logcat { "Min speed, increasing" }
//                val minForceIncrease = minSpeed
////                val maxForceSize = minForceIncrease / body.forceScale
//                val minForceSize = minForceIncrease * body.forceScale
//                val force =
//                    Vector().set(forward).normalize().multiply(forceMultiplier.let {
//                        if (it > 0) it.coerceAtLeast(minForceSize) else it.coerceAtMost(minForceSize)
//                    })
//
//
//                body.applyForce(
//                    Vec2(
//                        force.x,
//                        force.z
//                    ),
//                    Vec2(
//                        forward.x * (kart.properties.handling.forceOffset.orElse() ?: 0.0),
//                        forward.z * (kart.properties.handling.forceOffset.orElse() ?: 0.0)
//                    )
//                )
//            }
//        }

//        logcat { "ground=${kart.currentlyOnGround} isBraking=${isBraking} isManuallyHandbraking=$isManuallyHandbraking isHandbraking=$isHandbraking" }

        if (controller.forward() == 0f && kart.currentlyOnGround || isHandbraking) {
//            logcat { "Applying brakes" }
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
        body.angle += steerForce *
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

        val speed = interactor.speedInBpt
        if (speed != 0.0) {
            val yawPitch = LookAtUtil.YawPitch()
            LookAtUtil.getYawPitchFromRadian(kart.lastLocation, kart.location, yawPitch)
            val pitch = if (kart.lastLocation == kart.location) 0.0 else Math.toDegrees(yawPitch.pitch) + 90.0

            val properties = kart.properties
            val forwardForce = forwardVelocity.length()
            val controller = kart.controller
            val isDrivingBackwards =
                if (forwardForce in -0.001..0.001) controller.forward() < 0 else forward.dot(forwardVelocity) < 0
            val planeLifter = properties.planeLifter!!

            val forwardYaw = LookAtUtil.getYawToTarget(Vector(), forwardVelocity) ?: 0.0
            val rightYaw = LookAtUtil.getYawToTarget(Vector(), rightVelocity) ?: 0.0

            val angle = AngleUtils.distance(forwardYaw, rightYaw)

//            val angle = Math.toDegrees(forwardVelocity.angle(rightVelocity).toDouble())

//            logcat { "Velocity forward=${forwardVelocity.asString()} right=${rightVelocity.asString()}" }
            val sideForce = rightVelocity.length()
            val roll =
                (if (angle >= 0) -sideForce else sideForce) * planeLifter.steerForceRotation.force.translate(speed)
            matrix4x4.rotateYawPitchRoll(if (isDrivingBackwards) pitch else -pitch, 0.0, roll)
        }
    }
}