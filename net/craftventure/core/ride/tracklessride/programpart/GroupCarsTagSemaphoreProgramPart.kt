package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TagContainer
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ConditionType
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.programpart.data.TagContext
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class GroupCarsTagSemaphoreProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = GroupCarsTagSemaphoreProgramPart.type
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
        val targets: List<TagContainer> = group.cars.toList()
        val result = when (data.conditionType) {
            ConditionType.IS_SET ->
                if (targets.all { it.hasTag(TagContext.SCENE, data.value) })
                    ExecuteResult.DONE
                else
                    ExecuteResult.ON_HOLD
            ConditionType.IS_NOT_SET ->
                if (targets.none { it.hasTag(TagContext.SCENE, data.value) })
                    ExecuteResult.DONE
                else
                    ExecuteResult.ON_HOLD
        }
        if (result == ExecuteResult.DONE) {
            if (data.action == Action.CLEAR) {
                targets.forEach { it.removeTag(TagContext.SCENE, data.value) }
            }
        }
        return result
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val conditionType: ConditionType,
        val value: String,
        val action: Action = Action.IGNORE
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = GroupCarsTagSemaphoreProgramPart(this, scene)
    }

    enum class Action {
        CLEAR,
        IGNORE
    }

    companion object {
        const val type = "group_cars_tag_semaphore"
    }
}