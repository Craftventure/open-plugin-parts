package net.craftventure.core.ride.tracklessride.scene.trigger

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.TracklessRideController
import net.craftventure.core.ride.tracklessride.programpart.action.ActionData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class ProgressSceneTrigger(
    private val data: Data,
) : SceneTrigger() {
    override val continuity: Continuity = Continuity.CONTINUOUS
    private val actions = data.action.map { it.toAction() }
    override val forceRunOnCompletion: Boolean get() = data.forceRunOnCompletion

    companion object {
        const val type = "progress"
    }

    override fun shouldTrigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        route: TracklessRideController.ProgressedRoute
    ): Boolean = data.value in route.oldProgress..route.progress

    override fun trigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar
    ) {
        actions.forEach { it.execute(ride, group, car) }
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val value: Double,
        val action: List<ActionData>,
        val forceRunOnCompletion: Boolean = false,
    ) : SceneTriggerData() {
        override fun toTrigger(scene: TracklessRideScene): SceneTrigger = ProgressSceneTrigger(this)
    }
}