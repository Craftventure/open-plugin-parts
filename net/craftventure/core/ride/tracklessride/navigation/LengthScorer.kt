package net.craftventure.core.ride.tracklessride.navigation

class LengthScorer : Scorer {
    override fun score(from: GraphNode, to: GraphNode): Double {
        val shortest = from.shortestConnectionTo(to)
        if (shortest != null) return shortest.length
        return Double.POSITIVE_INFINITY
    }
}