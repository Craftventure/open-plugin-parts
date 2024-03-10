package net.craftventure.core.ride.tracklessride.programpart.action

import com.squareup.moshi.JsonClass
import net.craftventure.core.effect.EffectManager
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class SpecialEffectAction(
    private val data: Data,
) : Action() {
    override fun execute(ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar) {
        val effect = EffectManager.findByName(data.name)
        if (effect != null) {
            when (data.action) {
                ActionType.Start -> effect.play()
                ActionType.Stop -> effect.stop()
            }
        }
    }

    companion object {
        const val type = "specialeffect"
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val name: String,
        val action: ActionType
    ) : ActionData() {
        override fun toAction(): Action = SpecialEffectAction(this)
    }

    enum class ActionType {
        Start,
        Stop,
    }
}