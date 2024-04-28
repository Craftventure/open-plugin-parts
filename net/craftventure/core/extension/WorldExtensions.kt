package net.craftventure.core.extension

import net.craftventure.bukkit.ktx.extension.getBoundingBoxesForBlock
import net.craftventure.core.utils.BoundingBox
import org.bukkit.World
import kotlin.math.ceil
import kotlin.math.floor


fun World.collidingCheck(
    source: BoundingBox,
    calcBoundingBox: BoundingBox,
    offsetX: Double,
    offsetY: Double,
    offsetZ: Double,
    collideAction: ((x: Int, y: Int, z: Int, block: BoundingBox) -> Boolean) = { _, _, _, _ -> true }
): Boolean {
    var collided = false
    val xMin = floor(source.xMin + offsetX).toInt() - 1
    val yMin = floor(source.yMin + offsetY).toInt() - 1
    val zMin = floor(source.zMin + offsetZ).toInt() - 1

    val xMax = ceil(source.xMax + offsetX).toInt() + 1
    val yMax = ceil(source.yMax + offsetY).toInt() + 1
    val zMax = ceil(source.zMax + offsetZ).toInt() + 1

    for (x in xMin until xMax) {
        for (y in yMin until yMax)
            for (z in zMin until zMax) {
                val blockBoundingBoxes = getBoundingBoxesForBlock(x, y, z)
                if (blockBoundingBoxes != null) {
                    for (blockBoundingBox in blockBoundingBoxes) {
                        calcBoundingBox.set(blockBoundingBox)

                        val intersectsX = source.xIntersectsWithXOf(offsetX, x.toDouble(), calcBoundingBox)
                        val intersectsY = source.yIntersectsWithYOf(offsetY, y.toDouble(), calcBoundingBox)
                        val intersectsZ = source.zIntersectsWithZOf(offsetZ, z.toDouble(), calcBoundingBox)

                        if (intersectsX && intersectsY && intersectsZ) {
                            collided = true
                            if (collideAction.invoke(x, y, z, calcBoundingBox)) {
                                return true
                            }
                        }
                    }
                }
            }
    }

    return collided
}