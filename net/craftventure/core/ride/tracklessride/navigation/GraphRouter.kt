package net.craftventure.core.ride.tracklessride.navigation

import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.Logger
import org.bukkit.util.Vector
import java.util.*

class GraphRouter(
    private val graph: NavigationGraph,
    private val nextNodeScorer: Scorer = LengthScorer(),
    private val targetScorer: Scorer = DistanceScorer(),
) {
    private val cache = hashMapOf<Pair<String, String>, ArrayList<GraphNode>>()

    fun findRoute(from: GraphNode, to: GraphNode): Route? {
        cache[(from.id to to.id)]?.let {
//            Logger.debug("From cache")
            return Route(it)
        }

        val openSet = PriorityQueue<RouteNode>()
        val allNodes = hashMapOf<GraphNode, RouteNode>()

        val start = RouteNode(from, null, 0.0, targetScorer.score(from, to))
        openSet.add(start)
        allNodes[from] = start

        while (!openSet.isEmpty()) {
            val next = openSet.poll()
            if (next.current === to) {
                val route = arrayListOf<GraphNode>()
                var current = next
                do {
                    route.add(0, current.current)
                    current = allNodes.get(current.previous)
                } while (current != null)
                cache[(from.id to to.id)] = route
                return Route(route)
            }

            next.current.connections
//                .also { Logger.debug("Found ${it.size}") }
                .forEach { graphNodeConnection ->
//                    Logger.debug(
//                        " - From ${next.current.id} with " +
//                                "${graphNodeConnection.from.id} to ${graphNodeConnection.to.id} with score " +
//                                "${nextNodeScorer.score(graphNodeConnection.from, graphNodeConnection.to)}"
//                    )
                    val connection = graphNodeConnection.to
                    val nextNode = allNodes.getOrDefault(connection, RouteNode(connection))
                    allNodes.put(connection, nextNode)

                    val newScore =
                        next.routeScore + nextNodeScorer.score(/*next.current*/graphNodeConnection.from, connection)
                    if (newScore < nextNode.routeScore) {
                        nextNode.previous = next.current
                        nextNode.routeScore = newScore
                        nextNode.estimatedScore = newScore + targetScorer.score(connection, to)
                        openSet.add(nextNode)
                    }
                }
        }

        return null
    }

    data class Route(
        val route: ArrayList<GraphNode>
    ) {
        fun append(other: Route): Route = Route(ArrayList(this.route + other.route.drop(1)))

        val routeParts by lazy {
            val items = mutableListOf<GraphNodeConnection>()
            var firstNode = route.first()
            route.drop(1).forEach { currentNode ->
                try {
                    items += firstNode.shortestConnectionTo(currentNode)!!
                } catch (e: Exception) {
                    Logger.debug("Failed to find a connection from ${firstNode.id} to ${currentNode.id}")
                    throw e
                }
                firstNode = currentNode
            }
            return@lazy items
        }

        val length: Double by lazy { routeParts.sumOf { it.length } }

        fun locationAtDistance(distance: Double): Vector {
            if (length < distance || distance < 0) throw IllegalStateException("Distance ${distance.format(2)} is outside of route range")
            val parts = LinkedList(routeParts.toMutableList())
            var distanceOffset = 0.0
            while (parts.isNotEmpty()) {
                val part = parts.poll()
                val distanceOnPart = distance - distanceOffset
                if (part.length >= distanceOnPart) {
//                    Logger.debug(
//                        "${distance.format(2)}/${part.length.format(2)} > ${distanceOnPart.format(2)} > ${
//                            part.fromAt.format(
//                                2
//                            )
//                        }/${
//                            part.toAt.format(
//                                2
//                            )
//                        }"
//                    )
                    if (part.fromAt < part.toAt) {
                        return part.by.positionAt(part.fromAt + distanceOnPart)
                    } else {
                        return part.by.positionAt(part.fromAt - distanceOnPart)
                    }
                }
                distanceOffset += part.length
            }
            throw IllegalStateException("Failed to calculate distance in range")
        }

        fun pathPositionAtDistance(distance: Double): PathPosition {
            if (length < distance || distance < 0) throw IllegalStateException("Distance ${distance.format(2)} is outside of route range")
            val parts = LinkedList(routeParts.toMutableList())
            var distanceOffset = 0.0
            while (parts.isNotEmpty()) {
                val part = parts.poll()
                val distanceOnPart = distance - distanceOffset
                if (part.length >= distanceOnPart || parts.isEmpty()) {
//                    Logger.debug(
//                        "${distance.format(2)}/${part.length.format(2)} > ${distanceOnPart.format(2)} > ${
//                            part.fromAt.format(
//                                2
//                            )
//                        }/${
//                            part.toAt.format(
//                                2
//                            )
//                        }"
//                    )
                    if (part.fromAt < part.toAt) {
                        return PathPosition(part.by, part.fromAt + distanceOnPart)
                    } else {
                        return PathPosition(part.by, part.fromAt - distanceOnPart)
                    }
                }
                distanceOffset += part.length
            }
            throw IllegalStateException("Failed to calculate distance ${distance.format(5)} in range ${length.format(5)}")
        }
    }
}