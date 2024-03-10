package net.craftventure.core.ride.tracklessride.programpart

import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

abstract class ProgramPart<S>(
    protected val scene: TracklessRideScene
) {
    open val forceRunOnCompletion: Boolean get() = false
    abstract val type: String

    abstract fun createInitialState(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
    ): S

    abstract fun execute(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        state: S,
    ): ExecuteResult

    fun handleForceRun(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        state: S,
    ): ExecuteResult = ExecuteResult.DONE

    open fun clear(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar, state: S,
    ) {
    }

    open fun getPreviousProgress(state: S): Double = 0.0
    open fun getCurrentProgress(state: S): Double = 0.0

    enum class ExecuteResult {
        /**
         * The action was successfully executed. The only result that will actually make the controller advance to the next action
         */
        DONE,

        /**
         * Same like DONE, but will not continue the actions after this action on the same tick
         */
        DONE_AND_HALT,

        /**
         * This action has had it's progress changed
         */
        PROGRESS,

        /**
         * An error occured while executing this action
         */
        FAILED,

        /**
         * The action did not yet execute, most presumably the run conditions aren't met yet
         */
        ON_HOLD
    }
}