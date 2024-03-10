package net.craftventure.core.ride.tracklessride.programpart.action

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.craftventure.core.script.ScriptManager

class ScriptsAction(
    private val data: Data,
) : Action() {
    override fun execute(ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar) {
        data.value.forEach { action ->
            when (action.trigger) {
                TriggerType.START -> ScriptManager.start(action.group, action.name)
                TriggerType.STOP -> ScriptManager.stop(action.group, action.name)
                TriggerType.RESTART -> {
                    ScriptManager.stop(action.group, action.name)
                    ScriptManager.start(action.group, action.name)
                }
            }
        }
    }

    companion object {
        const val type = "scripts"
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val value: List<ScriptData>
    ) : ActionData() {
        override fun toAction(): Action = ScriptsAction(this)
    }

    @JsonClass(generateAdapter = true)
    class ScriptData(
        val trigger: TriggerType,
        val group: String,
        val name: String,
    )

    enum class TriggerType {
        START,
        STOP,
        RESTART
    }
}