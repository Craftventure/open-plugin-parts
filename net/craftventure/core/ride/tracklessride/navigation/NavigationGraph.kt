package net.craftventure.core.ride.tracklessride.navigation

import net.craftventure.core.ride.tracklessride.track.PathPart
import org.bukkit.util.Vector

class NavigationGraph {
    private val path = hashSetOf<PathPart>()
    private val nodes = hashSetOf<GraphNode>()
    private var prepared = false

    val isPrepared: Boolean
        get() = prepared

    val nodesList: List<GraphNode>
        get() = nodes.toList()

    private fun updateGraph() {
        path.sortedBy { it.id }.forEach {
            val start = it.positionAt(0.0)
            findNode(start) ?: createNode(start, "${it.id}_start")
            val end = it.positionAt(it.length)
            findNode(end) ?: createNode(end, "${it.id}_end")
        }

        path.sortedBy { it.id }.forEach {
            val start = it.positionAt(0.0)
            val startNode = findNode(start) ?: createNode(start, "${it.id}_start")
            val startResult = startNode.links.add(PathPosition(it, 0.0))
//            Logger.debug("Added link to start $startResult ${startNode.id} ${startNode.links.size}")

            val end = it.positionAt(it.length)
            val endNode = findNode(end) ?: createNode(end, "${it.id}_end")
            val endResult = endNode.links.add(PathPosition(it, it.length))
//            Logger.debug("Added link to end   $endResult ${endNode.id} ${endNode.links.size}")
        }

        val position = Vector(0, 0, 0)
        path.sortedBy { it.id }.forEach { part ->
            var i = 0.0
            while (i < part.length) {
                part.positionAt(i, position)
                nodes.forEach { node ->
                    if (node.contains(position)) {
                        if (node.links.none { it.part == part && it.at in (i - 1.0..i + 1.0) })
                            node.links.add(PathPosition(part, i))
                    }
                }
                i += 0.05
            }
        }

        nodes.forEach { node ->
            node.links.forEach { position ->
                val otherNodesAtPath = nodes
                    .filter { it !== node }
                    .flatMap { otherNode ->
                        otherNode.links.filter { it.part === position.part }.map { it to otherNode }
                    }
//                Logger.debug("Trying ${node.id} > ${otherNodesAtPath.map { "${it.second.id}/${it.first.part.id}/${it.first.at.format(2)}" }.joinToString(", ")}")
                val positiveMatch = otherNodesAtPath.filter { it.first.at > position.at }.minByOrNull { it.first.at }
                if (positiveMatch != null) {
//                    Logger.debug("  - Found positive ${positiveMatch.second.id}/${positiveMatch.first.part.id}/${positiveMatch.first.at.format(2)}")
                    node.connections.add(
                        GraphNodeConnection(
                            node,
                            positiveMatch.second,
                            position.part,
                            position.at,
                            positiveMatch.first.at
                        )
                    )
                }
                val negativeMatch = otherNodesAtPath.filter { it.first.at < position.at }.maxByOrNull { it.first.at }
                if (negativeMatch != null) {
//                    Logger.debug("  - Found negative ${negativeMatch.second.id}/${negativeMatch.first.part.id}/${negativeMatch.first.at.format(2)}")
                    node.connections.add(
                        GraphNodeConnection(
                            node,
                            negativeMatch.second,
                            position.part,
                            position.at,
                            negativeMatch.first.at
                        )
                    )
                }

//                Logger.debug("Node ${node.id} potential with ${otherNodesAtPath.size}${otherNodesAtPath.map { it.second. }} nodes")
            }
        }
    }

    fun toFirstPathPosition(node: GraphNode) = node.links.firstOrNull()
//        findConnectsFrom(node).firstOrNull()?.from?.link ?: findConnectsTo(node).firstOrNull()?.to?.link

    fun nearestNode(pathPosition: PathPosition): GraphNode {
        return nodes.minByOrNull { it.location.distanceSquared(pathPosition.location) }!!
//        val nodes = nodes.filter { it.links.any { it.part === pathPosition.part } }
    }

//    fun nearestAnyNodeLink(pathPosition: PathPosition): GraphNodeLink {
//        val quickLookup = graphNodeLinksList.firstOrNull { it.link == pathPosition }
//        if (quickLookup != null) return quickLookup
//        val location = pathPosition.location
//        val minBy = nodeLinks.minByOrNull { it.graphNode.location.distanceSquared(location) }
//        return minBy!!
//    }

    fun requirePrepared(): NavigationGraph {
        if (!prepared) prepare()
        return this
    }

    fun invalidate() {
        prepared = false
        nodes.forEach {
            it.links.clear()
            it.connections.clear()
        }
    }

    private fun addNode(node: GraphNode) {
        nodes.add(node)
        invalidate()
    }

    @Throws(IllegalStateException::class)
    fun prepare() {
        invalidate()
        updateGraph()
        prepared = true
    }

    fun addPathPart(pathPart: PathPart) {
        path.add(pathPart)
        invalidate()
    }

    fun findNode(id: String) = nodes.firstOrNull { it.id == id }
    fun findNode(where: Vector): GraphNode? = nodes.firstOrNull { it.contains(where) }

    fun createNode(
        where: Vector,
        id: String,
        dimensions: Vector = Vector(0.1, 0.1, 0.1)
    ): GraphNode =
        findNode(where)
            ?: findNode(id)
            ?: GraphNode(id, where.clone(), dimensions = dimensions).also { addNode(it) }
}