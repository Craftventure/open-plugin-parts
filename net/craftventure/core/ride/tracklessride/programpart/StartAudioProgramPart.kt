package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.api.AudioServerApi.addAndSync
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class StartAudioProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = StartAudioProgramPart.type
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
        car.playerPassengers.forEach {
            addAndSync(data.audioName, it, System.currentTimeMillis())
        }
        return ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val audioName: String,
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = StartAudioProgramPart(this, scene)
    }

    companion object {
        const val type = "start_audio"
    }
}