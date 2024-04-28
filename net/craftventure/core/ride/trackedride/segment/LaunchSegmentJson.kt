package net.craftventure.core.ride.trackedride.segment

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.trackedride.SplinedTrackSegmentJson
import net.craftventure.core.ride.trackedride.TrackSegment
import net.craftventure.core.ride.trackedride.TrackedRide

@JsonClass(generateAdapter = true)
open class LaunchSegmentJson : SplinedTrackSegmentJson() {
    var transportSpeed = 0.0
    var maxSpeed = 0.0
    var accelerateForce = 0.0
    var brakeForce = 0.0
    var launchTransportSpeed = 0.0
    var launchMaxSpeed = 0.0
    var launchAccelerateForce = 0.0
    var launchBrakeForce = 0.0
    var stationaryTicks = 0
    var frontCarStationaryPercentage = 0.0

//    var failedLaunchRetryBoostSpeed = -25.0
//    var failedLaunchTransportSpeed = 8.0
//    var failedLaunchBrakeForce = 0.9
//    var failedLaunchAccelerateMultiplier = 1.1

    override fun create(trackedRide: TrackedRide): TrackSegment =
        LaunchSegment(
            id,
            displayName,
            trackedRide,
            transportSpeed,
            accelerateForce,
            maxSpeed,
            brakeForce,
            launchTransportSpeed,
            launchAccelerateForce,
            launchMaxSpeed,
            launchBrakeForce,
            stationaryTicks,
            frontCarStationaryPercentage,
            isBlockSection
        ).apply {
            this.restore(this@LaunchSegmentJson)
        }
}