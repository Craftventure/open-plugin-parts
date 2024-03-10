package net.craftventure.core.ride.tracklessride.programpart.action

import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

abstract class Action {
    open val forceRunOnCompletion: Boolean get() = false

    abstract fun execute(ride: TracklessRide, group: TracklessRideCarGroup, car: TracklessRideCar)
}