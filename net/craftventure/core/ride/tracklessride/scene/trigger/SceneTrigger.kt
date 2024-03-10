package net.craftventure.core.ride.tracklessride.scene.trigger

import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.TracklessRideController
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

abstract class SceneTrigger {
    val id: Int = nextId
    abstract val continuity: Continuity
    abstract val forceRunOnCompletion: Boolean

    abstract fun shouldTrigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        route: TracklessRideController.ProgressedRoute
    ): Boolean

    abstract fun trigger(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar
    )

    enum class Continuity {
        /**
         * This trigger may be ran multiple times after each other, i.e.: don't use this to start/stop a scene, it may execute every single tick
         */
        CONTINUOUS,

        /**
         * This trigger should be run only once
         */
        ONCE
    }

    companion object {
        protected var nextId: Int = 0
            get() {
                field++
                return field
            }
    }
}