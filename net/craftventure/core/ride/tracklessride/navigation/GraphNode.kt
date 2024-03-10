package net.craftventure.core.ride.tracklessride.navigation

import com.squareup.moshi.JsonClass
import org.bukkit.util.Vector

data class GraphNode(
    val id: String,
    val location: Vector,
    val dimensions: Vector = Vector(0.1, 0.1, 0.1),
) {
    var links = mutableSetOf<PathPosition>()
    val connections = mutableSetOf<GraphNodeConnection>()

    fun shortestConnectionTo(other: GraphNode): GraphNodeConnection? {
        return connections
            .filter { it.to === other }
            .minByOrNull { it.length }
    }

    fun contains(vector: Vector) =
        vector.x in (location.x - dimensions.x.times(0.5))..(location.x + dimensions.x.times(0.5)) &&
                vector.y in (location.y - dimensions.y.times(0.5))..(location.y + dimensions.y.times(0.5)) &&
                vector.z in (location.z - dimensions.z.times(0.5))..(location.z + dimensions.z.times(0.5))

    @JsonClass(generateAdapter = true)
    data class Json(
        val id: String,
        val location: Vector,
        val dimensions: Vector = Vector(0.1, 0.1, 0.1),
    )
}