package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.DoubleSettingValueType
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.programpart.data.PropertyTargetType
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class SetDoublePropertyProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = SetDoublePropertyProgramPart.type
    override val forceRunOnCompletion: Boolean = data.forceRunOnCompletion

    override fun createInitialState(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
    ): Any = Unit

    override fun execute(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        state: Any
    ): ExecuteResult {
        logcat { "This action isn't implemented" }
//        when (data.to) {
//            PropertyTargetType.CAR -> TODO()
//            PropertyTargetType.GROUP -> TODO()
//            PropertyTargetType.CARS_IN_GROUP -> TODO()
//        }
        return ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val to: PropertyTargetType,
        val property: String,
        val valueType: DoubleSettingValueType,
        val value: Double,
        val easing: String?,
        val acceleration: Double?,
        val deceleration: Double?,
        val maxSpeed: Double?,
        val forceRunOnCompletion: Boolean = false
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = SetDoublePropertyProgramPart(this, scene)
    }

    companion object {
        const val type = "set_double_property"
    }
}