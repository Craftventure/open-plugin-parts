package net.craftventure.core.ride.tracklessride.programpart.action

import com.squareup.moshi.JsonClass
import net.craftventure.core.manager.DoorManager
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class DoorAction(
    private val data: Data,
) : Action() {
    override fun execute(ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar) {
        DoorManager.get(data.id)?.open(data.open, data.ticks)
    }

    companion object {
        const val type = "door_action"
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val id: String,
        val open: Boolean,
        val ticks: Int,
    ) : ActionData() {
        override fun toAction(): Action = DoorAction(this)
    }
}