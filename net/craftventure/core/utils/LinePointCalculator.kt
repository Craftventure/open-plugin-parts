package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.extension.set
import org.bukkit.util.Vector

class LinePointCalculator {
    private val origin: Vector = Vector()
    private val direction: Vector = Vector()
    private val point: Vector = Vector()

    fun nearestPointOnLine(origin: Vector, direction: Vector, point: Vector, out: Vector): Vector {
        this.origin.set(origin)
        this.direction.set(direction)
        this.point.set(point)

        this.direction.normalize()
        val v = this.point.subtract(this.origin)
        val d = v.dot(this.direction)
//        Logger.debug("d=${d.format(2)}")
        out.set(this.origin)
        return out.add(this.direction.multiply(d))
    }
}