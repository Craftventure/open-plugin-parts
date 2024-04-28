package net.craftventure.core.ride.trackedride

import com.squareup.moshi.JsonClass
import org.bukkit.util.Vector
import java.lang.Math.pow
import kotlin.math.sqrt

class SplineNode {
    var `in`: SplineHandle
    var knot: SplineHandle
    var out: SplineHandle
    var banking: Double

    constructor(splineNode: SplineNode) {
        banking = splineNode.banking
        `in` = SplineHandle(splineNode.`in`)
        knot = SplineHandle(splineNode.knot)
        out = SplineHandle(splineNode.out)
    }

    @JvmOverloads
    constructor(`in`: SplineHandle, knot: SplineHandle, out: SplineHandle, banking: Double = 0.0) {
        this.`in` = `in`
        this.knot = knot
        this.out = out
        this.banking = banking
    }

    fun calculateInAndOut(previous: SplineNode, next: SplineNode, t: Double) {
        val x0 = previous.knot.x
        val y0 = previous.knot.y
        val z0 = previous.knot.z
        val x1 = knot.x
        val y1 = knot.y
        val z1 = knot.z
        val x2 = next.knot.x
        val y2 = next.knot.y
        val z2 = next.knot.z
        val d01 = sqrt(pow(x1 - x0, 2.0) + pow(y1 - y0, 2.0) + pow(z1 - z0, 2.0))
        val d12 = sqrt(pow(x2 - x1, 2.0) + pow(y2 - y1, 2.0) + pow(z2 - z1, 2.0))
        val fa = t * d01 / (d01 + d12) // scaling factor for triangle Ta
        val fb = t * d12 / (d01 + d12) // ditto for Tb, simplifies to fb=t-fa
        val p1x = x1 - fa * (x2 - x0) // x2-x0 is the width of triangle T
        val p1y = y1 - fa * (y2 - y0) // y2-y0 is the height of T
        val p1z = z1 - fa * (z2 - z0) // y2-y0 is the height of T
        val p2x = x1 + fb * (x2 - x0)
        val p2y = y1 + fb * (y2 - y0)
        val p2z = z1 + fb * (z2 - z0)
        `in` = SplineHandle(p1x, p1y, p1z)
        out = SplineHandle(p2x, p2y, p2z)
    }

    @JsonClass(generateAdapter = true)
    data class Json(
        val `in`: Vector,
        val knot: Vector,
        val out: Vector,
        val banking: Double,
    ) {
        fun toSplineNode() = SplineNode(SplineHandle(`in`), SplineHandle(knot), SplineHandle(out), banking)
    }
}