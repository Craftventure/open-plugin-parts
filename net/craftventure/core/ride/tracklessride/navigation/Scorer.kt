package net.craftventure.core.ride.tracklessride.navigation

interface Scorer {
    fun score(from: GraphNode, to: GraphNode): Double
}