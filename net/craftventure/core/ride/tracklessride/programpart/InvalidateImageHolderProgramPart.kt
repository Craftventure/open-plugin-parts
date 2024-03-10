package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.map.renderer.MapManager
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class InvalidateImageHolderProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = Companion.type
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
        if (data.requirePlayers && group.playerPassengers.isEmpty()) {
            return ExecuteResult.DONE
        }
        data.ids.forEach { id ->
            val imageHolder = MapManager.instance.getImageHolder(id)
            if (imageHolder is TrainImageHolder)
                when (data.groupAction) {
                    GroupAction.SET -> {
                        imageHolder.group = group
                    }
                    GroupAction.CLEAR -> {
                        imageHolder.group = null
                    }
                    null -> {}
                }
            if (data.invalidateRender) {
                imageHolder?.invalidateRender()
            }
            if (data.invalidateSources) {
                imageHolder?.invalidateSources()
            }
        }
        return ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val ids: Set<String>,
        val invalidateRender: Boolean = false,
        val invalidateSources: Boolean = false,
        val groupAction: GroupAction? = null,
        val requirePlayers: Boolean = false,
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = InvalidateImageHolderProgramPart(this, scene)
    }

    enum class GroupAction {
        SET,
        CLEAR
    }

    interface TrainImageHolder {
        var group: TracklessRideCarGroup?
    }

    companion object {
        const val type = "image_holder_action"
    }
}