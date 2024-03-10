package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class WaitProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<WaitProgramPart.State>(scene) {
    override val type: String = WaitProgramPart.type
    override fun createInitialState(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
    ): State = State()

    override fun getPreviousProgress(state: State): Double = state.previousProgress
    override fun getCurrentProgress(state: State): Double = state.progress

    override fun execute(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        state: State
    ): ExecuteResult {
        val now = System.currentTimeMillis()
        val end = state.startTime + data.toMillis()
        if (now > end) return ExecuteResult.DONE
        state.progress = (now - state.startTime) / (end - state.startTime).toDouble()
        return ExecuteResult.PROGRESS
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val seconds: Double,
        val waitTime: WaitTimeType = WaitTimeType.TIME,
//        val triggers: List<SceneTriggerData>?,
    ) : ProgramPartData<State>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<State> = WaitProgramPart(this, scene)
        fun toMillis(): Long = (seconds * 1000).toLong()
    }

    enum class WaitTimeType {
        TIME
    }

    data class State(
        val startTime: Long = System.currentTimeMillis(),
    ) {
        var previousProgress: Double = 0.0
        var progress: Double = 0.0
            set(value) {
                previousProgress = field
                field = value
            }
    }

    companion object {
        const val type = "wait"
    }
}