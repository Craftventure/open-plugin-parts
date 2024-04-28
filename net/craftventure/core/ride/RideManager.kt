package net.craftventure.core.ride

import net.craftventure.core.ride.trackedride.FlatrideManager
import net.craftventure.core.ride.trackedride.TrackedRideManager
import net.craftventure.core.ride.trackedride.TracklessRideManager

object RideManager {
    var allowAutoDispatch = true

    fun getRide(id: String): RideInstance? {
        return TrackedRideManager.getTrackedRide(id) ?: TracklessRideManager.getRide(id) ?: FlatrideManager.getFlatride(
            id
        )
    }
}