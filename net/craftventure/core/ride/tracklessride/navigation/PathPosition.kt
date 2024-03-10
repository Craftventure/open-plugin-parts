package net.craftventure.core.ride.tracklessride.navigation

import net.craftventure.core.ride.tracklessride.track.PathPart

data class PathPosition(
    val part: PathPart,
    val at: Double
) {
    val location by lazy { part.positionAt(at) }
}