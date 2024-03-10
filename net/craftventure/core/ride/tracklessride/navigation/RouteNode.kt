package net.craftventure.core.ride.tracklessride.navigation

data class RouteNode(
    val current: GraphNode,
    var previous: GraphNode? = null,
    var routeScore: Double = Double.POSITIVE_INFINITY,
    var estimatedScore: Double = Double.POSITIVE_INFINITY,
) : Comparable<RouteNode> {
    override fun compareTo(other: RouteNode): Int = when {
        this.estimatedScore > other.estimatedScore -> 1
        this.estimatedScore < other.estimatedScore -> -1
        else -> 0
    }
}