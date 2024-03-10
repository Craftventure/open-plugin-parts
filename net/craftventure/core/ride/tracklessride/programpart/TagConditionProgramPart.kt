package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TagContainer
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ConditionType
import net.craftventure.core.ride.tracklessride.programpart.data.MatchContext
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.programpart.data.TagContext
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class TagConditionProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = TagConditionProgramPart.type
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
        val targets: List<TagContainer> = when (data.matchContext) {
            MatchContext.CAR -> listOf(car)
            MatchContext.GROUP -> listOf(group)
        }
        return when (data.conditionType) {
            ConditionType.IS_SET ->
                if (targets.all { it.hasTag(data.tagContext, data.value) })
                    ExecuteResult.DONE
                else
                    ExecuteResult.ON_HOLD
            ConditionType.IS_NOT_SET ->
                if (targets.none { it.hasTag(data.tagContext, data.value) })
                    ExecuteResult.DONE
                else
                    ExecuteResult.ON_HOLD
        }
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val conditionType: ConditionType,
        val matchContext: MatchContext,
        val tagContext: TagContext,
        val matchContextTarget: Int?,
        val value: String
    ) : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = TagConditionProgramPart(this, scene)
    }

    companion object {
        const val type = "tag_condition"
    }
}