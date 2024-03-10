package net.craftventure.core.ride.tracklessride.programpart.action

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.PropertyTargetType
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class SetBooleanPropertyAction(
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
        data.values.forEach { value ->
            car.getBooleanProperty(value.property)?.value = value.value
        }
    }

    companion object {
        const val type = "set_boolean_property"
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val to: PropertyTargetType,
        val values: List<Value>,
        val forceRun: Boolean = false
    ) : ActionData() {
        override fun toAction(): Action = SetBooleanPropertyAction(this)

        @JsonClass(generateAdapter = true)
        class Value(
            val property: String,
            val value: Boolean,
        )
    }
}