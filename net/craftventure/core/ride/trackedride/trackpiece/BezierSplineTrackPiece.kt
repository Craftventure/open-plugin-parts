package net.craftventure.core.ride.trackedride.trackpiece

import net.craftventure.core.ride.trackedride.SplineHelper
import net.craftventure.core.ride.trackedride.SplineNode

class BezierSplineTrackPiece @JvmOverloads constructor(
    val a: SplineNode,
    val b: SplineNode,
    val precisionFix: Boolean = false,
) {
    val length: Double
    private val tList = DoubleArray(100)
    var startT = 0.0
        set(value) {
            field = value
            tRange = endT - value
        }
    var endT = 0.0
        set(value) {
            field = value
            tRange = value - startT
        }
    var tRange = 0.0
        private set

    fun translateT(t: Double): Double {
        for (i in 0..98) {
            if (tList[i] <= t && tList[i + 1] >= t) {
                val deltaIndex = (t - tList[i]) / (tList[i + 1] - tList[i])
                val t1 = i.toDouble() / 100.0
                val t2 = (i + 1.0) / 100.0
                val deltaTs = t2 - t1
                return t1 + deltaIndex * deltaTs
            }
        }
        return t
    }

    init {
        var length = 0.0
        for (i in 0..(if (precisionFix) 99 else 98)) {
            val index = i.toDouble()
            val position1 = SplineHelper.getValue(
                index / 100.0,
                a.knot.toVector(),
                a.out.toVector(),
                b.`in`.toVector(),
                b.knot.toVector()
            )
            val position2 = SplineHelper.getValue(
                index / 100.0 + 1.0 / 100.0,
                a.knot.toVector(),
                a.out.toVector(),
                b.`in`.toVector(),
                b.knot.toVector()
            )
            length += position1.distance(position2)
        }
        this.length = length
        length = 0.0
        tList[0] = 0.0
        for (i in 0..98) {
            val index = i.toDouble()
            val position1 = SplineHelper.getValue(
                index / 100.0,
                a.knot.toVector(),
                a.out.toVector(),
                b.`in`.toVector(),
                b.knot.toVector()
            )
            val position2 = SplineHelper.getValue(
                index / 100.0 + 1.0 / 100.0,
                a.knot.toVector(),
                a.out.toVector(),
                b.`in`.toVector(),
                b.knot.toVector()
            )
            length += position1.distance(position2)
            tList[i + 1] = length / this.length
        }
    }
}