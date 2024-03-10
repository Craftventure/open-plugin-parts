package net.craftventure.core.ride.tracklessride.track

import org.bukkit.util.Vector

abstract class PathPart {
    abstract val id: String

    //    val nodes: Set<NodeLink>
    abstract val length: Double

//    fun linkNode(node: Node, at: Double)
//    fun unlinkNode(node: Node)

    open fun positionAt(distance: Double): Vector = Vector().also { positionAt(distance, it) }
    abstract fun positionAt(distance: Double, vector: Vector)

    abstract class Data {
        abstract fun toPathPart(): PathPart
    }
}