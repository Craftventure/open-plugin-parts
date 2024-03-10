package net.craftventure.core.map

import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ride.trackedride.TrackedRide
import org.bukkit.util.Vector

object CoordTranslator {
    class WorldCoordTranslator private constructor(
        targetMin: Vector,
        targetMax: Vector,
        xMultiply: Double,
        zMultiply: Double,
        padding: Int,
        horizontalBias: Double,
        verticalBias: Double,
        val mapSize: Int = 128
    ) {
        val min: Vector
        val max: Vector

        init {
            val targetMinMapCoords = Vector(
                padding + (mapSize * ((1 - xMultiply) * horizontalBias)) + 0.0 * (mapSize - (padding * 2)) * xMultiply,
                0.0,
                padding + (mapSize * ((1 - zMultiply) * verticalBias)) + 0.0 * (mapSize - (padding * 2)) * zMultiply
            )
            val targetMaxMapCoords = Vector(
                padding + (mapSize * ((1 - xMultiply) * horizontalBias)) + 1.0 * (mapSize - (padding * 2)) * xMultiply,
                0.0,
                padding + (mapSize * ((1 - zMultiply) * verticalBias)) + 1.0 * (mapSize - (padding * 2)) * zMultiply
            )
            val pixelSize = ((targetMax.x - targetMin.x) / (targetMaxMapCoords.x - targetMinMapCoords.x))
//            Logger.debug("targetMin=${targetMin.asString()} targetMax=${targetMax.asString()}")
//            Logger.debug("targetMinMapCoords=${targetMinMapCoords.asString()} targetMaxMapCoords=${targetMaxMapCoords.asString()}")
//            Logger.debug("pixelSize=${pixelSize.format(2)}")
            min = Vector(
                targetMin.x - (targetMinMapCoords.x * pixelSize),
                targetMin.y,
                targetMin.z - (targetMinMapCoords.z * pixelSize)
            )
            max = Vector(
                targetMax.x + ((mapSize - targetMaxMapCoords.x) * pixelSize),
                targetMax.y,
                targetMax.z + ((mapSize - targetMaxMapCoords.z) * pixelSize)
            )
//            Logger.debug("min=${min.asString()} max=${max.asString()}")
        }

        fun getWorldX(mapX: Int) = getWorldX(mapX.toDouble())
        fun getWorldX(mapX: Double): Double = ((mapX / mapSize.toDouble()) * (max.x - min.x)) + min.x
        fun getWorldZ(mapZ: Int) = getWorldZ(mapZ.toDouble())
        fun getWorldZ(mapZ: Double): Double = ((mapZ / mapSize.toDouble()) * (max.z - min.z)) + min.z

        fun getMapX(worldX: Double): Double = ((worldX - min.x) / (max.x - min.x)) * mapSize
        fun getMapZ(worldZ: Double): Double = ((worldZ - min.z) / (max.z - min.z)) * mapSize

//        fun toCoord(location: Location, padding: Int = 0): Coord = Coord(getMapX(location.x, padding).toLong(), getMapZ(location.z, padding).toLong())

        companion object {
            fun fromTrackedRide(
                trackedRide: TrackedRide,
                padding: Int,
                horizontalBias: Double,
                verticalBias: Double
            ): WorldCoordTranslator {
                val segments = ArrayList(trackedRide.trackSegments)
                val min = Vector()
                segments.first().getPosition(0.0, min)
                val max = Vector()
                segments.first().getPosition(0.0, max)

                val calculations = Vector()

                for (subsegments in segments.map { it.subsegments })
                    segments.addAll(subsegments)

                for (segment in segments) {
                    for (i in 0..Math.floor(segment.length).toInt() * 2) {
                        segment.getPosition(i.toDouble() / 2.0, calculations)
                        min.x = Math.min(min.x, calculations.x)
//                    min.y = Math.min(min.y, calculations.y)
                        min.z = Math.min(min.z, calculations.z)

                        max.x = Math.max(max.x, calculations.x)
//                    max.y = Math.max(min.y, calculations.y)
                        max.z = Math.max(max.z, calculations.z)
                    }
                }

                val totalX = max.x - min.x
                val totalZ = max.z - min.z

                val xMultiply = when {
                    totalX > totalZ -> 1.0
                    else -> totalX / totalZ
                }
                val zMultiply = when {
                    totalZ > totalX -> 1.0
                    else -> totalZ / totalX
                }

                return WorldCoordTranslator(min, max, xMultiply, zMultiply, padding, horizontalBias, verticalBias)
            }
        }
    }

//    class Coord(
//            val x: Long,
//            val y: Long
//    ) {
//        fun isOnMap() = x in 0..128 && 6 in 0..128
//    }

    fun yawToCursorRotation(yawIn: Double): Byte {
        var yaw = yawIn
        while (yaw < 0)
            yaw += 360
        yaw %= 360

        return (Math.abs(4 + ((Math.round(yaw) / 360.0) * 16.0)) % 16).toInt().toByte()
    }

    fun xCoordToCursorCoord(x: Int) = (-128 + (x * 2)).clamp(-128, 127).toByte()
    fun yCoordToCursorCoord(y: Int) = (-128 + (y * 2)).clamp(-128, 127).toByte()

    fun translateCursorCoordinate(x: Int, y: Int) = CursorCoord((-128 + (x * 2)).toByte(), (-128 + (y * 2)).toByte())

    class CursorCoord(
        val x: Byte,
        val y: Byte
    ) {
        fun isOnMap() = x in 0..128 && 6 in 0..128
    }
}