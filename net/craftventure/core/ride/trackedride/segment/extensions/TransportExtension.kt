package net.craftventure.core.ride.trackedride.segment.extensions

import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.TrackSegment
import net.craftventure.core.ride.trackedride.segment.ExtensibleSegment
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class TransportExtension(
    var transportSpeed: Double,
    var accelerateForce: Double,
    var maxSpeed: Double,
    var brakeForce: Double,
    override var enabled: Boolean = true
) : ExtensibleSegment.Extension {
    private var isAffectedByEmergency = false
    var isUseTrainTargetSpeed = false
    var isUseTrainTargetSpeedAsMaxSpeed = false

    override var attachedSegment: TrackSegment? = null

    var interceptor: Interceptor? = null
    var appendAcceleration = false

    fun setAffectedByEmergency(affectedByEmergency: Boolean) {
        isAffectedByEmergency = affectedByEmergency
    }

    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
        super.applyForces(car, distanceSinceLastUpdate)
        if ((interceptor == null || interceptor!!.shouldApply(
                car,
                distanceSinceLastUpdate
            )) && (!isAffectedByEmergency || !trackedRide!!.isEmergencyStopActive)
        ) {
            val originalAcceleration = car.acceleration
            val targetSpeed =
                if (isUseTrainTargetSpeed) car.attachedTrain.targetSpeed ?: transportSpeed else transportSpeed
            val dynamicMaxSpeed =
                if (isUseTrainTargetSpeed && isUseTrainTargetSpeedAsMaxSpeed) targetSpeed else maxSpeed
            if (targetSpeed > 0) {
                if (car.velocity + car.acceleration < targetSpeed) {
                    car.acceleration = min(accelerateForce, max(targetSpeed - car.velocity, 0.0))
                }
            } else if (targetSpeed < 0) {
                if (car.velocity + car.acceleration > targetSpeed) {
                    car.acceleration = -min(accelerateForce, max(-targetSpeed - car.velocity, 0.0))
                }
            }

            if (dynamicMaxSpeed >= 0.0 && abs(car.velocity) + abs(car.acceleration) > dynamicMaxSpeed) {
                if (car.velocity > dynamicMaxSpeed) {
                    car.acceleration = -min(brakeForce, car.velocity - dynamicMaxSpeed)
                } else {
                    car.acceleration = min(brakeForce, dynamicMaxSpeed - car.velocity)
                }
            }

            if (appendAcceleration) {
                car.acceleration = originalAcceleration + car.acceleration
            }
        }
    }

    interface Interceptor {
        fun shouldApply(car: RideCar, distanceSinceLastUpdate: Double): Boolean
    }
}