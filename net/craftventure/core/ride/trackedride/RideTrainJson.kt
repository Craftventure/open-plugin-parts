package net.craftventure.core.ride.trackedride

abstract class RideTrainJson {
    lateinit var spawnSegmentId: String
    var spawnDistance: Double = 0.0

    abstract fun create(ride: TrackedRide): RideTrain
}