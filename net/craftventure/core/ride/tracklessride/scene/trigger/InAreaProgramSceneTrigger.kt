package net.craftventure.core.ride.tracklessride.scene.trigger

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.TracklessRideController
import net.craftventure.core.ride.tracklessride.programpart.action.ActionData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class InAreaProgramSceneTrigger(
    private val data: Data,
) : SceneTrigger() {
    private val area = data.area.create()
    override val continuity: Continuity = Continuity.ONCE
    private val actions = data.action.map { it.toAction() }
    override val forceRunOnCompletion: Boolean get() = data.forceRunOnCompletion

    companion object {
        const val type = "in_area"
    }

    override fun shouldTrigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        route: TracklessRideController.ProgressedRoute
    ): Boolean = car.pathPosition.location in area

    override fun trigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar
    ) {
        actions.forEach { it.execute(ride, group, car) }
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val area: Area.Json,
        val action: List<ActionData>,
        val forceRunOnCompletion: Boolean = false,
    ) : SceneTriggerData() {
        override fun toTrigger(scene: TracklessRideScene): SceneTrigger = InAreaProgramSceneTrigger(this)
    }
}