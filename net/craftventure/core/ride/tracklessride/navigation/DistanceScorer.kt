package net.craftventure.core.ride.tracklessride.navigation

class DistanceScorer : Scorer {
    override fun score(from: GraphNode, to: GraphNode): Double {
        return from.location.distance(to.location)
    }
}