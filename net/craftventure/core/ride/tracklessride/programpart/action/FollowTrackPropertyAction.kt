package net.craftventure.core.ride.tracklessride.programpart.action

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.isEffectivelyZeroBy4Decimals
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.PropertyTargetType
import net.craftventure.core.ride.tracklessride.property.DoubleProperty
import net.craftventure.core.ride.tracklessride.property.DoublePropertyAnimator
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class FollowTrackPropertyAction(
    private val data: Data,
) : Action() {
    override val forceRunOnCompletion: Boolean = data.forceRun

    override fun execute(ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar) {
//        Logger.debug("Executing action")
        when (data.to) {
            PropertyTargetType.CAR -> {
                animate(ride, group, car)
            }

            PropertyTargetType.GROUP -> {
            }

            PropertyTargetType.CARS_IN_GROUP -> group.cars.forEach { groupCar ->
                animate(ride, group, groupCar)
            }
        }
    }

    private fun animate(ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar) {
        var rotateSpeed: Double = 0.0

        car.animateProperty(
            data.property,
            DoublePropertyAnimator { timeDeltaMs: Long, property: DoubleProperty ->
                val currentYaw = property.value
                val targetYaw = car.lastTrackYaw?.let {
                    val initial = it + data.yawOffsetDegrees
                    val diff = AngleUtils.distance(
                        AngleUtils.clampDegrees(currentYaw),
                        AngleUtils.clampDegrees(initial)
                    )
                    initial + (if (data.isBiDirectional && diff !in -90.0..90.0
                    ) {
//                        logcat { "Compensation ${car.idInGroup} with ${diff.format(2)} curr=${currentYaw.format(2)} target=${initial.format(2)}" }
                        180.0
                    } else 0.0)
                }
//                logcat { "Target=${targetYaw?.format(2)}" }
                if (targetYaw != null) {
                    /** Can be negative */
                    val currentStopDistance =
                        if (rotateSpeed.isEffectivelyZeroBy4Decimals || data.speedIncreasePerTick == 0.0) {
                            0.0
                        } else {
                            var distance = 0.0
                            var speed = rotateSpeed

                            if (rotateSpeed > 0) {
                                while (speed > 0) {
                                    speed -= data.speedIncreasePerTick
                                    distance += speed
                                }
                            } else {
                                while (speed < 0) {
                                    speed += data.speedIncreasePerTick
                                    distance += speed
                                }
                            }
                            distance
                        }


                    // TODO: Use currentYawPlusStop in case we reverse a direction and in the same time the distance becomes opposite positivity
                    val distance =
                        AngleUtils.distance(
                            AngleUtils.clampDegrees(currentYaw),
                            AngleUtils.clampDegrees(targetYaw)
                        )

//                    logcat {
//                        "${car.group.groupId}/${car.idInGroup}: distance=${
//                            AngleUtils.distance(
//                                AngleUtils.clampDegrees(property.value),
//                                AngleUtils.clampDegrees(targetYaw)
//                            ).format(2)
//                        } actualDistance=${distance.format(2)}"
//                    }

                    if (distance > 0) {
                        if (distance < currentStopDistance)
                            rotateSpeed -= data.speedIncreasePerTick
                        else
                            rotateSpeed += data.speedIncreasePerTick
                        rotateSpeed = rotateSpeed.clamp(-data.maxSpeedPerTick, data.maxSpeedPerTick)
                        if (rotateSpeed > distance) {
                            rotateSpeed = 0.0
                        }
                    } else if (distance < 0) {
                        if (distance > currentStopDistance)
                            rotateSpeed += data.speedIncreasePerTick
                        else
                            rotateSpeed -= data.speedIncreasePerTick
                        rotateSpeed = rotateSpeed.clamp(-data.maxSpeedPerTick, data.maxSpeedPerTick)
                        if (rotateSpeed < distance) {
                            rotateSpeed = 0.0
                        }
                    }

                    property.value += rotateSpeed

//                    logcat {
//                        "${group.groupId}/${car.idInGroup} Animating to ${property.value?.format(2)} at speed ${
//                            rotateSpeed?.format(
//                                2
//                            )
//                        }"
//                    }
                }
                false
            })
    }

    companion object {
        const val type = "follow_track_property"
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val to: PropertyTargetType,
        val property: String,
        val yawOffsetDegrees: Double = 0.0,
        val maxSpeedPerTick: Double = 90.0 / 20.0,
        val speedIncreasePerTick: Double = 15 / 20.0,
        val isBiDirectional: Boolean = false,
        val forceRun: Boolean = false
    ) : ActionData() {
        override fun toAction(): Action = FollowTrackPropertyAction(this)
    }
}