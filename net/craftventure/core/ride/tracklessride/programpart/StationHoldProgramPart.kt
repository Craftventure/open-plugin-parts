package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.scene.TracklessStationScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class StationHoldProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<StationHoldProgramPart.State>(scene) {
    override val type: String = StationHoldProgramPart.type
    override fun createInitialState(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
    ): State = State()

    override fun execute(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        state: State
    ): ExecuteResult {
        val scene = group.currentScene
        return if (scene is TracklessStationScene) {
            if (scene.state == TracklessStationScene.State.DISPATCHING)
                ExecuteResult.DONE
            else
                ExecuteResult.ON_HOLD
        } else
            ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    class Data : ProgramPartData<State>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<State> = StationHoldProgramPart(this, scene)
    }

    data class State(
        val startTime: Long = System.currentTimeMillis()
    )

    companion object {
        const val type = "station_hold"
    }
}