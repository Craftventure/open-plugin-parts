package net.craftventure.core.ride.tracklessride.programpart.action

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.DoubleSettingValueType
import net.craftventure.core.ride.tracklessride.programpart.data.PropertyTargetType
import net.craftventure.core.ride.tracklessride.property.DoubleProperty
import net.craftventure.core.ride.tracklessride.property.DoublePropertyAnimator
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import penner.easing.Easing
import java.util.*
import kotlin.math.abs

class SetDoublePropertyAction(
    private val data: Data,
) : Action() {
    override val forceRunOnCompletion: Boolean = data.forceRun
    private val easing: Easing = data.easing?.let { Easing.byId(it) } ?: Easing.LINEAR.also {
        Logger.warn("Failed to find easing ${data.easing} for action with data $data, falling back to LINEAR")
    }
    private val ease: Easing.Ease =
        easing.easyByType(data.easingType?.let { Easing.EasingType.valueOf(it.uppercase(Locale.getDefault())) }
            ?: Easing.EasingType.EASE_IN_OUT.also {
                Logger.warn("Failed to find easingType ${data.easingType} for action with data $data, falling back to EASE_IN_OUT")
            })

    override fun execute(ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar) {
//        Logger.debug("Executing action")
        when (data.to) {
            PropertyTargetType.CAR -> {
                data.values.forEach { value ->
                    animate(value, ride, group, car)
                }
            }
            PropertyTargetType.GROUP -> {
            }
            PropertyTargetType.CARS_IN_GROUP -> group.cars.forEach { groupCar ->
                data.values.forEach { value ->
                    animate(value, ride, group, groupCar)
                }
            }
        }
    }

    private fun animate(value: Data.Value, ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar) {
        if (data.durationMs == null)
            car.getDoubleProperty(value.property)?.value = value.value
        else {
            val property = car.getDoubleProperty(value.property) ?: return
            val startValue = AngleUtils.clampDegrees(property.value)

            val diff: Double = when {
                data.useShortestPath -> {
                    val target =
                        if (data.valueType == DoubleSettingValueType.RELATIVE) startValue + value.value else value.value
                    val clampedTargetValue = AngleUtils.clampDegrees(target)
                    val diff = AngleUtils.distance(startValue, target)
//                    Logger.debug("Shortest from ${startValue.format(2)} to ${target.format(2)} (${clampedTargetValue.format(2)}) is ${diff.format(2)}")
                    diff
                }
                else -> {
                    when {
                        data.valueType == DoubleSettingValueType.RELATIVE -> value.value
                        startValue < value.value -> abs(startValue - value.value)
                        else -> -abs(startValue - value.value)
                    }
                }
            }
            val targetValue =
                if (data.valueType == DoubleSettingValueType.RELATIVE) startValue + value.value else value.value
//                    val angle = AngleUtils.smallestMoveTo()
            val startTime = System.currentTimeMillis()
//            Logger.debug("Animate ${startValue.format(2)} > ${data.value.format(2)} (${targetValue.format(2)})")
            car.animateProperty(
                value.property,
                DoublePropertyAnimator { timeDeltaMs: Long, property: DoubleProperty ->
                    val time = System.currentTimeMillis() - startTime
                    if (time >= data.durationMs) {
//                        Logger.debug("Finish ${property.value.format(2)} > ${targetValue.format(2)}")
                        property.value = AngleUtils.clampDegrees(targetValue)
                        true
                    } else {
//                        Logger.debug("Value ${property.value.format(2)}")
                        property.value = AngleUtils.clampDegrees(
                            startValue + ease.ease(
                                time.toDouble(),
                                0.0,
                                diff,
                                data.durationMs.toDouble()
                            )
                        )
                        false
                    }
                })
        }
    }

    companion object {
        const val type = "set_double_property"
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val to: PropertyTargetType,
        val valueType: DoubleSettingValueType = DoubleSettingValueType.ABSOLUTE,
        val values: List<Value>,
//        val animator: String?, // easing, hardcoded one
        val easing: String? = null,
        val easingType: String? = null,
        val durationMs: Long? = null,
        val useShortestPath: Boolean = true,
        val forceRun: Boolean = false
    ) : ActionData() {
        override fun toAction(): Action = SetDoublePropertyAction(this)

        @JsonClass(generateAdapter = true)
        class Value(
            val property: String,
            val value: Double,
        )
    }
}