package net.craftventure.core.ride.trackedride.segment

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.trackedride.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A basic transport segment that has a minimum speed and a brake speed. The transport speed can be set to 0 to disable the effect, so can the brake speed.
 * The brake force can be used to set the strength of the applied brake effect if used
 *
 *
 * Can also be used as a trimbrake
 */
open class TransportSegment(
    id: String,
    displayName: String,
    trackedRide: TrackedRide,
    var transportSpeed: Double,
    var accelerateForce: Double,
    var maxSpeed: Double,
    var brakeForce: Double
) : SplinedTrackSegment(id, displayName, trackedRide) {
    private var acceleration: Acceleration? = null
    var isTransportEnabled = true
    private var isAffectedByEmergency = false
    var isUseTrainTargetSpeed = false
    var isUseTrainTargetSpeedAsMaxSpeed = false

    constructor(
        id: String,
        trackedRide: TrackedRide,
        transportSpeed: Double,
        accelerateForce: Double,
        maxSpeed: Double,
        brakeForce: Double
    ) : this(id, id, trackedRide, transportSpeed, accelerateForce, maxSpeed, brakeForce)

    constructor(id: String, base: TransportSegment) : this(
        id,
        id,
        base.trackedRide,
        base.transportSpeed,
        base.accelerateForce,
        base.maxSpeed,
        base.brakeForce
    )

    fun setAffectedByEmergency(affectedByEmergency: Boolean) {
        isAffectedByEmergency = affectedByEmergency
    }

    fun setAcceleration(acceleration: Acceleration) {
        this.acceleration = acceleration
    }

    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
        super.applyForces(car, distanceSinceLastUpdate)
        if ((!isAffectedByEmergency || !trackedRide.isEmergencyStopActive) && isTransportEnabled) {
            var targetSpeed =
                if (isUseTrainTargetSpeed) car.attachedTrain.targetSpeed ?: transportSpeed else transportSpeed
            val dynamicMaxSpeed =
                if (isUseTrainTargetSpeed && isUseTrainTargetSpeedAsMaxSpeed) targetSpeed else maxSpeed
            if (acceleration != null) {
                if (car.distance > acceleration!!.distanceFromStart && car.distance < car.trackSegment!!.length - acceleration!!.distanceFromEnd) {
                    if (!acceleration!!.onlyIfCanAdvance || canAdvanceToNextBlock(car.attachedTrain, false))
                        targetSpeed = acceleration!!.speed
                }
            }
            if (targetSpeed > 0) {
                if (car.velocity + car.acceleration < targetSpeed) {
                    car.acceleration = min(accelerateForce, max(targetSpeed - car.velocity, 0.0))
                }
            } else if (targetSpeed < 0) {
                if (car.velocity + car.acceleration > targetSpeed) {
                    car.acceleration = -min(accelerateForce, max(-targetSpeed - car.velocity, 0.0))
                }
            }

            if (dynamicMaxSpeed != 0.0 && abs(car.velocity) + abs(car.acceleration) > dynamicMaxSpeed) {
                if (car.velocity > dynamicMaxSpeed) {
                    car.acceleration = -min(brakeForce, car.velocity - dynamicMaxSpeed)
                } else {
                    car.acceleration = min(brakeForce, dynamicMaxSpeed - car.velocity)
                }
            }
        } else {
            car.velocity = 0.0
            car.acceleration = 0.0
        }
    }

    override fun toJson(): Json {
        val json = Json()
        return toJson(json)
    }

    override fun <T : TrackSegmentJson?> toJson(source: T): T & Any {
        source as Json
        source.transportSpeed = transportSpeed
        source.accelerateForce = accelerateForce
        source.maxSpeed = maxSpeed
        source.brakeForce = brakeForce
        source.isTransportEnabled = isTransportEnabled
        source.isAffectedByEmergency = isAffectedByEmergency
        source.isUseTrainTargetSpeed = isUseTrainTargetSpeed
        source.isUseTrainTargetSpeedAsMaxSpeed = isUseTrainTargetSpeedAsMaxSpeed
        return super.toJson(source)
    }

    override fun <T : TrackSegmentJson?> restore(source: T) {
        super.restore(source)
        source as Json

        isTransportEnabled = source.isTransportEnabled
        isAffectedByEmergency = source.isAffectedByEmergency
        isUseTrainTargetSpeed = source.isUseTrainTargetSpeed
        isUseTrainTargetSpeedAsMaxSpeed = source.isUseTrainTargetSpeedAsMaxSpeed

        if (source.acceleration != null)
            acceleration = source.acceleration
    }

    @JsonClass(generateAdapter = true)
    class Acceleration(
        val distanceFromStart: Double,
        val distanceFromEnd: Double,
        val speed: Double,
        val onlyIfCanAdvance: Boolean
    )

    @JsonClass(generateAdapter = true)
    open class Json : SplinedTrackSegmentJson() {
        var transportSpeed: Double = 0.0
        var accelerateForce: Double = 0.0
        var maxSpeed: Double = 0.0
        var brakeForce: Double = 0.0

        var isTransportEnabled = true
        var isAffectedByEmergency = false
        var isUseTrainTargetSpeed = false
        var isUseTrainTargetSpeedAsMaxSpeed = false

        var acceleration: Acceleration? = null

        override fun create(trackedRide: TrackedRide): TrackSegment =
            TransportSegment(
                id,
                displayName,
                trackedRide,
                transportSpeed,
                accelerateForce,
                maxSpeed,
                brakeForce
            ).apply {
                this.restore(this@Json)
            }
    }
}
