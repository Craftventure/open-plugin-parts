package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class ExitsToOverrideProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = ExitsToOverrideProgramPart.type
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
        group.exitToOverrides = data.value
        return ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val value: List<String>,
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = ExitsToOverrideProgramPart(this, scene)
    }

    companion object {
        const val type = "exits_to_override"
    }
}