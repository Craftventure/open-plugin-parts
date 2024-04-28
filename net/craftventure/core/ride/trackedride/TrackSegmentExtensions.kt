package net.craftventure.core.ride.trackedride

import org.bukkit.util.Vector

fun TrackSegment.getDistanceClosestTo(location: Vector, precision: Double = 0.05, startDistance: Double = 0.0): Double {
    val position = Vector()
    var i = startDistance
    var distanceAtSegment = 0.0
    var distanceFromTarget = 100000000.0
    while (i < length) {
        getPosition(i, position)
        val positionDistance = location.distance(position)
        if (positionDistance < distanceFromTarget) {
            distanceAtSegment = i
            distanceFromTarget = positionDistance
        }
        i += precision
    }
    return distanceAtSegment
}