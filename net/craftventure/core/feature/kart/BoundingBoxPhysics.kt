package net.craftventure.core.feature.kart

import net.craftventure.core.extension.collidingCheck
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.extension.isEffectivelyZeroBy4Decimals
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.utils.BoundingBox
import org.bukkit.World
import org.bukkit.util.Vector
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// This stuff can probably be done way easier, but hey, it works

object BoundingBoxPhysics {
    fun move(size: BoundingBox, world: World, location: Vector, velocity2: Vector, maxClimb: Double): PhysicsResult? {
        val calcBoundingBox = BoundingBox()
        val calcBoundingBox2 = BoundingBox()
        val offset = Vector()
        var currentlyOnGround = false
        val stepSize = 0.5
        val steps = ceil(
            max(
                velocity2.x.absoluteValue,
                max(velocity2.y.absoluteValue, velocity2.z.absoluteValue)
            ) / stepSize
        ).toInt()

        var currentClimb = 0.0
        for (step in 0 until (steps + if (currentClimb != 0.0) 1 else 0)) {
            val xStep = min(stepSize, max(-stepSize, velocity2.x - offset.x))
            val yStep = min(stepSize, max(-stepSize, velocity2.y - offset.y))
            val zStep = min(stepSize, max(-stepSize, velocity2.z - offset.z))

            if (xStep != 0.0) {
                var minX: Double? = null
                var maxX: Double? = null
                val didCollideX = world
                    .collidingCheck(
                        size,
                        calcBoundingBox,
                        location.x + offset.x + xStep,
                        location.y + offset.y + currentClimb,
                        location.z + offset.z
                    )
                    { x, y, z, boundingBox ->
                        //                                boundingBox.debug(world!!, x.toDouble(), y.toDouble(), z.toDouble())

//                                Logger.debug("Hit block at $x $y $z bb=$boundingBox")
                        val climb = Math.max(currentClimb, (y + boundingBox.yMax) - (location.y + offset.y))
                        if (climb != currentClimb) {
                            val isClimbable = climb in 0.0..maxClimb

//                                    Logger.debug("ClimbX ${climb.format(2)} isClimbable=$isClimbable")
                            if (isClimbable) {
                                val isSafe = !world.collidingCheck(
                                    size,
                                    calcBoundingBox2,
                                    location.x + offset.x + xStep,
                                    location.y + offset.y + climb,
                                    location.z + offset.z
                                )

//                                        Logger.debug("safeX=$isSafe y=${(location.y + offset.y + climb).format(2)}")
                                if (isSafe) {
                                    currentClimb = climb
                                    offset.x += xStep
                                    return@collidingCheck true
                                }
                            }
                        }
//                                Logger.debug(//"x=${x.format(2)} " +
//                                        "xOrigin=${(location.x + offset.x).format(2)} " +
//                                        "xStep=${xStep.format(2)} " +
//                                        "xMin=${(x + boundingBox.xMin).format(2)} " +
//                                                "xMax=${(x + boundingBox.xMax).format(2)} " +
//                                                "a=${(location.x + offset.x + xStep + properties.boundingBox.xMin).format(2)} " +
//                                                "b=${(location.x + offset.x + xStep + properties.boundingBox.xMax).format(2)}")

                        if (x + boundingBox.xMin < location.x + offset.x + xStep + size.xMax) {
                            val min = x + boundingBox.xMin
//                                    Logger.debug("max=${min.format(2)} existing=${maxX?.format(2)} bb=$boundingBox")
                            maxX = Math.min(maxX ?: min, min)
                        }
                        if (x + boundingBox.xMax > location.x + offset.x + xStep + size.xMin) {
                            val max = x + boundingBox.xMax
//                                    Logger.debug("min=${max.format(2)} existing=${minX?.format(2)} bb=$boundingBox")
                            minX = Math.max(minX ?: max, max)
                        }

                        if (maxX == null && minX == null) {
//                                    Logger.debug("Failed to resolve xStep")
                        }

                        false
                    }
                if (!didCollideX) {
                    offset.x += xStep
                } else {
//                        Logger.debug("Range ${(location.x + offset.x + xStep + properties.boundingBox.xMin).format(2)}..${(location.x + offset.x + xStep + properties.boundingBox.xMax).format(2)} collding with ${minX?.format(2)}..${maxX?.format(2)}")
//                        Logger.debug("minX=${minX?.format(2)} maxX=${maxX?.format(2)} for step=${xStep.format(2)}")
                    if (xStep < 0 && minX != null) {
                        val fixedStep =
                            xStep - ((location.x + offset.x + xStep + size.xMin) - minX!!)
                        if (fixedStep != zStep)
//                                Logger.debug("fixedStep=${fixedStep.format(2)} xStep=${xStep.format(2)}")
//                            Logger.debug("minX=${minX!!.format(2)} fixedStep=${fixedStep.format(2)} x=${location.x + offset.x} xStepped=${(location.x + offset.x + xStep)} xStep=${xStep.format(2)}")
                            if (xStep < 0 && fixedStep < 0 || xStep > 0 && fixedStep > 0) {
                                offset.x += fixedStep
                            } else {
//                                    Logger.debug("Cancelling xStep for xStep=${xStep.format(2)} fixedStep=${fixedStep.format(2)} minX=${minX!!.format(2)}")
//                                    offset.x = 0.0
                            }
                    } else if (xStep > 0 && maxX != null) {
                        val fixedStep =
                            xStep - ((location.x + offset.x + xStep + size.xMax) - maxX!!)
                        if (fixedStep != zStep)
//                                Logger.debug("fixedStep=${fixedStep.format(2)} xStep=${xStep.format(2)}")
//                            Logger.debug("maxX=${maxX!!.format(2)} fixedStep=${fixedStep.format(2)} x=${(location.x + offset.x).format(2)} xStepped=${(location.x + offset.x + xStep).format(2)} xStep=${xStep.format(2)}")
                            if (xStep < 0 && fixedStep < 0 || xStep > 0 && fixedStep > 0) {
                                offset.x += fixedStep
                            } else {
//                                    Logger.debug("Cancelling xStep for xStep=${xStep.format(2)} fixedStep=${fixedStep.format(2)} maxX=${maxX!!.format(2)}")
//                                    offset.x = 0.0
                            }
                    } else if (currentClimb == 0.0) {
//                            Logger.debug("Cancelling xStep for failed resolve xStep=${xStep.format(2)} minX=${minX?.format(2)} maxX=${maxX?.format(2)}")
                        offset.x = 0.0
                    }
                }
            }

            if (zStep != 0.0) {
                var minZ: Double? = null
                var maxZ: Double? = null
                val didCollideZ = world
                    .collidingCheck(
                        size,
                        calcBoundingBox,
                        location.x + offset.x,
                        location.y + offset.y + currentClimb,
                        location.z + offset.z + zStep
                    )
                    { x, y, z, boundingBox ->
                        //                                boundingBox.debug(world!!, x.toDouble(), y.toDouble(), z.toDouble())

                        val climb = Math.max(currentClimb, (y + boundingBox.yMax) - (location.y + offset.y))
                        if (climb != currentClimb) {
                            val isClimbable = climb in 0.0..maxClimb

//                                    Logger.debug("ClimbZ ${climb.format(2)} isClimbable=$isClimbable")
                            if (isClimbable) {
                                val isSafe = !world.collidingCheck(
                                    size,
                                    calcBoundingBox2,
                                    location.x + offset.x,
                                    location.y + offset.y + climb,
                                    location.z + offset.z + zStep
                                )

//                                        Logger.debug("safeZ=$isSafe y=${(location.y + offset.y + climb).format(2)}")
                                if (isSafe) {
                                    currentClimb = climb
                                    offset.z += zStep
                                    return@collidingCheck true
                                }
                            }
                        }
//                                Logger.debug("z=${z.format(2)} " +
//                                        "zOrigin=${(location.z + offset.z).format(2)} " +
//                                        "zStep=${zStep.format(2)} " +
//                                        "zMin=${(z + boundingBox.zMin).format(2)} " +
//                                        "zMax=${(z + boundingBox.zMax).format(2)} " +
//                                        "a=${(location.z + offset.z + zStep + properties.boundingBox.zMin).format(2)} " +
//                                        "b=${(location.z + offset.z + zStep + properties.boundingBox.zMax).format(2)}")

                        if (z + boundingBox.zMin < location.z + offset.z + zStep + size.zMax) {
                            val min = z + boundingBox.zMin
                            maxZ = Math.min(maxZ ?: min, min)
                        }
                        if (z + boundingBox.zMax > location.z + offset.z + zStep + size.zMin) {
                            val max = z + boundingBox.zMax
                            minZ = Math.max(minZ ?: max, max)
                        }
                        false
                    }
                if (!didCollideZ) {
                    offset.z += zStep
                } else {
//                        Logger.debug("minZ=${minZ?.format(2)} maxZ=${maxZ?.format(2)} for step=${zStep.format(2)}")
                    if (zStep < 0 && minZ != null) {
                        val fixedStep =
                            zStep - ((location.z + offset.z + zStep + size.zMin) - minZ!!)
                        if (fixedStep != zStep)
//                                Logger.debug("fixedStep=${fixedStep.format(2)} zStep=${zStep.format(2)}")
//                            Logger.debug("minZ=${minZ!!.format(2)} fixedStep=${fixedStep.format(2)} z=${location.z + offset.z} zStepped=${(location.z + offset.z + zStep)} zStep=${zStep.format(2)}")
                            if (zStep < 0 && fixedStep < 0 || zStep > 0 && fixedStep > 0) {
                                offset.z += fixedStep
                            } else {
//                                Logger.debug("Cancelling zStep")
//                                    offset.z = 0.0
                            }
                    } else if (zStep > 0 && maxZ != null) {
                        val fixedStep =
                            zStep - ((location.z + offset.z + zStep + size.zMax) - maxZ!!)
                        if (fixedStep != zStep)
//                                Logger.debug("fixedStep=${fixedStep.format(2)} zStep=${zStep.format(2)}")
//                            Logger.debug("maxZ=${maxZ!!.format(2)} fixedStep=${fixedStep.format(2)} z=${(location.z + offset.z).format(2)} zStepped=${(location.z + offset.z + zStep).format(2)} zStep=${zStep.format(2)}")
                            if (zStep < 0 && fixedStep < 0 || zStep > 0 && fixedStep > 0) {
                                offset.z += fixedStep
                            } else {
//                                Logger.debug("Cancelling zStep")
//                                    offset.z = 0.0
                            }
                    } else if (currentClimb == 0.0) {
//                            Logger.debug("Cancelling zStep for failed resolve zStep=${zStep.format(2)} minZ=${minZ?.format(2)} maxZ=${maxZ?.format(2)}")
                        offset.z = 0.0
                    }
                }
            }

//                val didCollideX = world!!.collidingCheck(
//                        properties.boundingBox,
//                        calcBoundingBox,
//                        location.x + offset.x + xStep,
//                        location.y + offset.y + currentClimb,
//                        location.z + offset.z)
//                { x, y, z, boundingBox ->
//                }


            if (currentClimb <= 0.0) {
                var minY: Double? = null
                var maxY: Double? = null
                val didCollideY = world.collidingCheck(
                    size,
                    calcBoundingBox,
                    location.x + offset.x,
                    location.y + offset.y + yStep,
                    location.z + offset.z
                )
                { x, y, z, boundingBox ->
                    //                        Logger.debug("Collides with $x $y $z")
//                        boundingBox.debug(world!!, x.toDouble(), y.toDouble(), z.toDouble())
//                    val margin = (location.y + offset.y + currentClimb + yStep) - (y + boundingBox.yMax)
                    if (y + boundingBox.yMax > location.y + offset.y + yStep + size.yMin) {
                        val max = y + boundingBox.yMax
                        minY = Math.max(minY ?: max, max)
                    } else if (y + boundingBox.xMin < location.y + offset.y + yStep + size.yMax) {
                        val min = y.toDouble() + boundingBox.yMin
                        maxY = Math.min(maxY ?: min, min)
                    }

                    currentlyOnGround = true
//                    logcat { "Block hit at $x $y $z bb=$boundingBox" }
//                        Logger.debug("Hit block at y=$y bb=$boundingBox")
//                    Logger.debug("Space left at y=$y maxY=${boundingBox.yMax} margin=$margin")
                    false
                }
                if (!didCollideY) {
                    offset.y += yStep
                } else {
                    if (yStep < 0 && minY != null) {
                        val fixedStep =
                            yStep - ((location.y + offset.y + yStep + size.yMin) - minY!!)
//                            Logger.debug("minY=${minY!!.format(2)} fixedStep=${fixedStep.format(2)} y=${location.y + offset.y} yStepped=${(location.y + offset.y + yStep)} yStep=${yStep.format(2)}")
                        if (yStep < 0 && fixedStep < 0 || yStep > 0 && fixedStep > 0) {
                            offset.y += fixedStep
                        } else {
//                                offset.y = 0.0
//                                Logger.debug("Y- halted")
                        }
                    } else if (yStep > 0 && maxY != null) {
                        val fixedStep =
                            yStep - ((location.y + offset.y + yStep + size.yMax) - maxY!!)
//                            Logger.debug("maxY=${maxY!!.format(2)} fixedStep=${fixedStep.format(2)} y=${(location.y + offset.y).format(2)} yStepped=${(location.y + offset.y + yStep).format(2)} yStep=${yStep.format(2)}")
                        if (yStep < 0 && fixedStep < 0 || yStep > 0 && fixedStep > 0) {
                            offset.y += fixedStep
                        } else {
//                                Logger.debug("Y+ halted")
//                                offset.y = 0.0
                        }
                    } else {
//                            Logger.debug("minY=${minY?.format(2)} maxY=${maxY?.format(2)}")
                        offset.y = 0.0
                        currentlyOnGround = true
                    }
                }
            }
//                offset.x += xStep

//                Logger.debug("y=${(location.y + offset.y).format(2)} yStep=${yStep.format(2)}")
//                Logger.debug("step=$step xStep=${xStep.format(2)} yStep=${yStep.format(2)} zStep=${zStep.format(2)}")
        }
//        location.y += currentClimb

//        currentlyOnGround = currentlyOnGround || velocity2.y <= 0 && currentClimb > 0.0
//        logcat {
//            "currentlyOnGround=$currentlyOnGround (was ${currentClimb != 0.0 || offset.y.isEffectivelyZeroBy4Decimals}) currentClimb=${
//                currentClimb.format(
//                    4
//                )
//            } ${offset.y.format(5)}"
//        }
        return PhysicsResult(
            offset,
            currentlyOnGround,
            currentClimb
        )
    }

    data class PhysicsResult(
        val actualMoved: Vector,
        val currentlyOnGround: Boolean,
        val currentClimb: Double
    )
}