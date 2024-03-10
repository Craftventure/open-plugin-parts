package net.craftventure.core.ride.tracklessride.track

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.ktx.util.Logger.warn
import net.craftventure.core.ride.trackedride.SplineHelper
import net.craftventure.core.ride.trackedride.SplineNode
import net.craftventure.core.ride.trackedride.trackpiece.BezierSplineTrackPiece
import org.bukkit.util.Vector

class SplinedPathPart(
    override val id: String
) : PathPart() {
    //    override val nodes: MutableSet<NodeLink> = mutableSetOf()
    override var length: Double = -1.0
        private set
        get() {
            if (field == -1.0) {
                field = 0.0
                for (i in trackPieceList.indices) {
                    val trackPiece = trackPieceList[i]
                    field += trackPiece.length
                }
                var currentT = 0.0
                for (i in trackPieceList.indices) {
                    val trackPiece = trackPieceList[i]
                    trackPiece.startT = currentT
                    currentT += trackPiece.length / field
                    trackPiece.endT = currentT
                }
            }
            return field
        }
    private val trackPieceList = mutableListOf<BezierSplineTrackPiece>()

    val trackPieceCount: Int
        get() = trackPieceList.size

//    override fun linkNode(node: Node, at: Double) {
//        val exists = nodes.any { it.node === node }
//        if (exists) return
//        nodes.add(NodeLink(node, at))
//    }
//
//    override fun unlinkNode(node: Node) {
//        nodes.removeIf { it.node === node }
//    }

    fun add(offset: Vector, vararg nodes: SplineNode) {
        val clonedNodes = nodes.map { SplineNode(it) }
        for (node in clonedNodes) {
            node.`in`.addOffset(offset.x, offset.y, offset.z)
            node.knot.addOffset(offset.x, offset.y, offset.z)
            node.out.addOffset(offset.x, offset.y, offset.z)
        }
        for (i in 1 until clonedNodes.size) {
            trackPieceList.add(BezierSplineTrackPiece(clonedNodes[i - 1], clonedNodes[i], true))
        }
    }

    fun add(vararg nodes: SplineNode) {
        this.add(Vector(), *nodes)
    }

    private fun getPosition(distance: Double, position: Vector) {
//        Logger.debug("Getting distance=${distance.format(2)} at $id")
        if (distance <= 0.0) {
//            Logger.debug("Using distance=0=a")
            position.set(trackPieceList.first().a.knot.toVector())
            return
        }
        if (distance >= length) {
//            Logger.debug("Using distance=length=b")
            position.set(trackPieceList.last().b.knot.toVector())
            return
        }
        var startDistance = 0.0
        trackPieceList.forEach { trackPiece ->
            if (distance in startDistance..(startDistance + trackPiece.length)) {
                val t = (distance - startDistance) / trackPiece.length
                if (t == 0.0) {
//                    Logger.debug("Using t=0=a")
                    position.set(trackPiece.a.knot.toVector())
                    return
                }
                if (t == 1.0) {
//                    Logger.debug("Using t=1=b")
                    position.set(trackPiece.b.knot.toVector())
                    return
                }
//                Logger.debug("Using translation ${t.format(2)}=${trackPiece.translateT(t).format(2)}")
                SplineHelper.getValue(
                    trackPiece.translateT(t),
                    trackPiece.a.knot.toVector(), trackPiece.a.out.toVector(),
                    trackPiece.b.`in`.toVector(), trackPiece.b.knot.toVector(),
                    position
                )
                return
            }
            startDistance += trackPiece.length
        }
//        var currentDistance = 0.0
//        for (i in trackPieceList.indices) {
//            val trackPiece = trackPieceList[i]
//            if (currentDistance <= distance && distance <= currentDistance + trackPiece.length) {
//                val t: Double =
//                    if (distance == currentDistance) 1.0 else (distance - currentDistance) / trackPiece.length
//
//                Logger.debug("Using translation ${t.format(2)}=${trackPiece.translateT(t).format(2)}")
//                SplineHelper.getValue(
//                    trackPiece.translateT(t),
//                    trackPiece.a.knot.toVector(), trackPiece.a.out.toVector(),
//                    trackPiece.b.getIn().toVector(), trackPiece.b.knot.toVector(),
//                    position
//                )
//                return
//            }
//            currentDistance += trackPiece.length
//        }
        warn("Failed to find position for distance $distance")
    }

    override fun positionAt(distance: Double, vector: Vector) = getPosition(distance, vector)


    @JsonClass(generateAdapter = true)
    data class Data(
        val id: String,
        val nodes: List<SplineNode.Json>
    ) : PathPart.Data() {
        override fun toPathPart(): PathPart = SplinedPathPart(id).apply {
            this.add(*nodes.map { it.toSplineNode() }.toTypedArray())
        }
    }

    companion object {
        const val type = "bezier"
    }
}