package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class SwitchSceneProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = SwitchSceneProgramPart.type
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
        if (data.requiredTotalScore != null) {
            val totalScore = group.cars.sumOf { it.team?.score ?: 0 }
            if (totalScore < data.requiredTotalScore) {
                return ExecuteResult.DONE
            }
        }
        data.value.forEach { ride.controller.queueToScene(group, it) }
        group.allowPrematureSceneSwitching = true
        return ExecuteResult.ON_HOLD
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val value: List<String>,
        val semaphoreTags: List<String>?,
        val requiredTotalScore: Int? = null,
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = SwitchSceneProgramPart(this, scene)
    }

    companion object {
        const val type = "switch_scene"
    }
}