package net.craftventure.core.ride.tracklessride.scene.trigger

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.TracklessRideController
import net.craftventure.core.ride.tracklessride.programpart.action.ActionData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class AtAbsoluteDistanceSceneTrigger(
    private val data: Data,
) : SceneTrigger() {
    override val continuity: Continuity = Continuity.ONCE
    private val actions = data.action.map { it.toAction() }
    override val forceRunOnCompletion: Boolean get() = data.forceRunOnCompletion

    companion object {
        const val type = "at_absolute_distance"
    }

    override fun shouldTrigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        route: TracklessRideController.ProgressedRoute
    ): Boolean = if (data.value > 0) data.value in route.distanceOld..route.distance
    else (route.route.length + data.value) in route.distanceOld..route.distance

    override fun trigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar
    ) {
        actions.forEach {
            it.execute(ride, group, car)
        }
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val value: Double,
        val action: List<ActionData>,
        val forceRunOnCompletion: Boolean = false,
    ) : SceneTriggerData() {
        override fun toTrigger(scene: TracklessRideScene): SceneTrigger = AtAbsoluteDistanceSceneTrigger(this)
    }
}