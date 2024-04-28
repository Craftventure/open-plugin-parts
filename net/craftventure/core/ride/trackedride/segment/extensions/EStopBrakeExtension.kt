package net.craftventure.core.ride.trackedride.segment.extensions

import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.TrackSegment
import net.craftventure.core.ride.trackedride.segment.ExtensibleSegment

open class EStopBrakeExtension(
    var brakeForce: Double,
    override var enabled: Boolean = true
) : ExtensibleSegment.Extension {

    override var attachedSegment: TrackSegment? = null

    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
        super.applyForces(car, distanceSinceLastUpdate)
        if (trackedRide!!.isEmergencyStopActive) {
            val carVelocity = car.velocity
            if (carVelocity == 0.0 || (car.velocity > 0 && car.velocity - brakeForce < 0) || (car.velocity < 0 && car.velocity + brakeForce > 0)) {
                Logger.debug("Holding velocity=${carVelocity.format(2)} brakeForce=${brakeForce.format(2)}")
                car.acceleration = 0.0
                car.velocity = 0.0
            } else if (carVelocity > 0) {
                Logger.debug("Applying brake velocity=${carVelocity.format(2)} brakeForce=${brakeForce.format(2)}")
                car.acceleration = -brakeForce
            } else {
                Logger.debug("Applying brake velocity=${carVelocity.format(2)} brakeForce=${brakeForce.format(2)}")
                car.acceleration = brakeForce
            }
        }
    }
}