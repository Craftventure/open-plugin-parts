package net.craftventure.core.ride.tracklessride.navigation

import net.craftventure.core.ride.tracklessride.track.PathPart

data class GraphNodeConnection(
    val from: GraphNode,
    val to: GraphNode,
    val by: PathPart,
    val fromAt: Double,
    val toAt: Double,
) {
    val length = if (fromAt < toAt) toAt - fromAt else fromAt - toAt
}