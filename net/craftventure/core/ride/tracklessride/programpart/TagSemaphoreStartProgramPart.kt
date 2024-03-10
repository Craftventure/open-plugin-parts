package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.programpart.data.TagContext
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class TagSemaphoreStartProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = TagSemaphoreStartProgramPart.type
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
        val groups = ride.activeGroups()
        val canUse = groups.all { otherGroup ->
            if (otherGroup === group) return@all true
            data.value.none { value ->
                otherGroup.hasTag(TagContext.GLOBAL, value)
            }
        }
//        Logger.debug("canUse=$canUse ${data.value.joinToString(", ")}")
        return if (canUse) {
            data.value.forEach { group.addTag(TagContext.GLOBAL, it) }
            ExecuteResult.DONE
        } else {
            ExecuteResult.ON_HOLD
        }
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val value: Array<String>
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = TagSemaphoreStartProgramPart(this, scene)
    }

    companion object {
        const val type = "tag_semaphore_start"
    }
}