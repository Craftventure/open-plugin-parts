package net.craftventure.core.effect

import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.utils.MathUtil
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import penner.easing.Quad


class SpaceMountainCannonEffect : BaseEffect("smcannon") {
    private val areaTracker: NpcAreaTracker
    private var state = State.IDLE
    private var ticksInState = 0

    private var forwardVector = Vector(0, 0, 0)
    private val rightVector = Vector(0, -1, 0)
    private val upVector = Vector(0, -1, 0)
    private val bankingVector = Vector(-1, 0, 0)
    private val particleLocation = Location(Bukkit.getWorld("world"), 245.0, 54.5, -762.5)
    private val cannonBlockList = ArrayList<CannonBlock>()

    init {
        areaTracker = NpcAreaTracker(SimpleArea("world", 178.0, 0.0, -835.0, 280.0, 255.0, -728.0))


        val banking = 0.0
        val workYaw = Math.toRadians(-90.0)//trackYawRadian + (Math.PI * 0.5);
        val workPitch = Math.toRadians(43.0)//trackPitchRadian + (Math.PI * 0.5);

        forwardVector = MathUtil.setYawPitchRadians(forwardVector, workYaw, workPitch)
        forwardVector.multiply(-1)
        forwardVector.y = forwardVector.y * -1
        forwardVector.normalize()

        upVector.setX(0)
        upVector.setY(-1)
        upVector.setZ(0)

        rightVector.setX(0)
        rightVector.setY(-1)
        rightVector.setZ(0)

        bankingVector.setX(-1)
        bankingVector.setY(0)
        bankingVector.setZ(0)
        MathUtil.setYawPitchRadians(bankingVector, workYaw, 0.0)
        MathUtil.rotate(rightVector, bankingVector, -banking * MathUtil.DEGTORAD)
        rightVector.crossProduct(forwardVector)
        rightVector.normalize()

        MathUtil.rotate(upVector, bankingVector, -banking * MathUtil.DEGTORAD - Math.PI * 0.5)
        upVector.crossProduct(forwardVector)
        upVector.normalize()

        addRedBlock(Location(Bukkit.getWorld("world"), 247.5, 54.0, -765.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 246.5, 55.0, -765.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 246.5, 56.0, -765.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 245.5, 56.0, -765.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 245.5, 57.0, -764.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 57.0, -764.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 57.0, -763.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 57.0, -762.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 243.5, 57.0, -762.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 57.0, -761.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 57.0, -760.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 245.5, 57.0, -760.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 245.5, 56.0, -759.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 246.5, 56.0, -759.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 246.5, 55.0, -759.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 247.5, 54.0, -759.5))


        addYellowBlock(Location(Bukkit.getWorld("world"), 246.5, 54.0, -765.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 245.5, 55.0, -765.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 244.5, 56.0, -764.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 243.5, 56.0, -763.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 242.5, 56.0, -763.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 242.5, 56.0, -761.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 243.5, 56.0, -761.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 244.5, 56.0, -760.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 245.5, 55.0, -759.5))
        addYellowBlock(Location(Bukkit.getWorld("world"), 246.5, 54.0, -759.5))

        addRedBlock(Location(Bukkit.getWorld("world"), 245.5, 54.0, -765.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 55.0, -765.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 243.5, 56.0, -764.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 242.5, 56.0, -762.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 243.5, 56.0, -760.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 55.0, -759.5))
        addRedBlock(Location(Bukkit.getWorld("world"), 245.5, 54.0, -759.5))

        addYellowBlock(Location(Bukkit.getWorld("world"), 245.5, 53.0, -765.5), false)
        addYellowBlock(Location(Bukkit.getWorld("world"), 244.5, 54.0, -765.5), false)
        addYellowBlock(Location(Bukkit.getWorld("world"), 243.5, 55.0, -764.5), false)
        addYellowBlock(Location(Bukkit.getWorld("world"), 243.5, 55.0, -760.5), false)
        addYellowBlock(Location(Bukkit.getWorld("world"), 244.5, 54.0, -759.5), false)
        addYellowBlock(Location(Bukkit.getWorld("world"), 245.5, 53.0, -759.5), false)

        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 53.0, -765.5), false)
        addRedBlock(Location(Bukkit.getWorld("world"), 243.5, 54.0, -765.5), false)
        addRedBlock(Location(Bukkit.getWorld("world"), 242.5, 55.0, -764.5), false)
        addRedBlock(Location(Bukkit.getWorld("world"), 242.5, 55.0, -760.5), false)
        addRedBlock(Location(Bukkit.getWorld("world"), 243.5, 54.0, -759.5), false)
        addRedBlock(Location(Bukkit.getWorld("world"), 244.5, 53.0, -759.5), false)
    }

    private fun addYellowBlock(location: Location, animated: Boolean = true) {
        val npcEntity = NpcEntity("spaceMountainCannon", EntityType.FALLING_BLOCK, location)
        npcEntity.setBlockData(Material.YELLOW_TERRACOTTA.createBlockData())
        npcEntity.noGravity(true)
        val cannonBlock = CannonBlock(npcEntity, animated, Vector(0, 0, 0), location)
        areaTracker.addEntity(npcEntity)
        cannonBlockList.add(cannonBlock)
    }

    private fun addRedBlock(location: Location, animated: Boolean = true) {
        val npcEntity = NpcEntity("spaceMountainCannon", EntityType.FALLING_BLOCK, location)
        npcEntity.setBlockData(Material.TERRACOTTA.createBlockData())
        npcEntity.noGravity(true)
        val cannonBlock = CannonBlock(npcEntity, animated, Vector(0, 0, 0), location)
        areaTracker.addEntity(npcEntity)
        cannonBlockList.add(cannonBlock)
    }

    override fun onStarted() {
        super.onStarted()
        areaTracker.startTracking()
        state = State.IDLE
        ticksInState = 0

        for (cannonBlock in cannonBlockList) {
            cannonBlock.originalLocation.block.type = Material.AIR
        }
    }

    override fun onStopped() {
        super.onStopped()
        areaTracker.stopTracking()

        for (cannonBlock in cannonBlockList) {
            val block = cannonBlock.originalLocation.block
            block.blockData = cannonBlock.blockNpc.blockData!!
        }
    }


    override fun update(tick: Int) {
        val cannonMoveDistance = if (state == State.SUBTRACTING) Quad.easeInOut(
            ticksInState.toDouble(),
            0.0,
            1.0,
            (20 * 2).toDouble()
        ) else if (state == State.EXTRACTING) 1 - ticksInState / (20 * 4.0) else 0.0
        if (state != State.EXTRACTING) {
            val count = if (state == State.IDLE) 10 else 18
            val size = 3.0
            for (i in 0 until count) {
                val angle = i / count.toDouble()
                if (angle < 0.65 || angle > 0.85) {
                    val rightOffset = Math.cos(angle * Math.PI * 2.0) * size
                    val upOffset = Math.sin(angle * Math.PI * 2.0) * size
                    particleLocation.world?.spawnParticleX(
                        Particle.EXPLOSION_NORMAL,
                        particleLocation.x - calculateX(
                            rightVector,
                            upVector,
                            forwardVector,
                            rightOffset,
                            upOffset,
                            2.5 - cannonMoveDistance * 1.41
                        ),
                        particleLocation.y - calculateY(
                            rightVector,
                            upVector,
                            forwardVector,
                            rightOffset,
                            upOffset,
                            2.5 - cannonMoveDistance * 1.41
                        ),
                        particleLocation.z - calculateZ(
                            rightVector,
                            upVector,
                            forwardVector,
                            rightOffset,
                            upOffset,
                            2.5 - cannonMoveDistance * 1.41
                        ),
                        if (state == State.SUBTRACTING) 2 else 1,
                        if (state == State.SUBTRACTING) 0.5 else 0.1,
                        if (state == State.SUBTRACTING) 0.5 else 0.1,
                        if (state == State.SUBTRACTING) 0.5 else 0.1
                    )
                }
            }
        }
        for (cannonBlock in cannonBlockList) {
            val distance = if (cannonBlock.isShouldAnimate) cannonMoveDistance else 0.0
            cannonBlock.blockNpc.move(
                cannonBlock.originalLocation.x - distance,
                cannonBlock.originalLocation.y - distance,
                if (cannonBlock.originalLocation.y < 57 && cannonBlock.originalLocation.z < -762.5 && cannonBlock.isShouldAnimate)
                    cannonBlock.originalLocation.z - 0.05
                else if (cannonBlock.originalLocation.y < 57 && cannonBlock.originalLocation.z > -762.5 && cannonBlock.isShouldAnimate)
                    cannonBlock.originalLocation.z + 0.05
                else
                    cannonBlock.originalLocation.z
            )
        }

        if (state == State.IDLE && ticksInState > 20 * 5) {
            ticksInState = 0
            state = State.SUBTRACTING
        } else if (state == State.SUBTRACTING && ticksInState > 20 * 2) {
            ticksInState = 0
            state = State.EXTRACTING
        } else if (state == State.EXTRACTING && ticksInState > 20 * 4) {
            stop()
            return
        }

        ticksInState++
        if (tick > 15 * 20) {
            stop()
        }
    }

    private fun calculateX(
        rightVector: Vector,
        upVector: Vector,
        forwardVector: Vector,
        right: Double,
        up: Double,
        forward: Double
    ): Double {
        return rightVector.x * right + upVector.x * up + forwardVector.x * forward
    }

    private fun calculateY(
        rightVector: Vector,
        upVector: Vector,
        forwardVector: Vector,
        right: Double,
        up: Double,
        forward: Double
    ): Double {
        return rightVector.y * right + upVector.y * up + forwardVector.y * forward
    }

    private fun calculateZ(
        rightVector: Vector,
        upVector: Vector,
        forwardVector: Vector,
        right: Double,
        up: Double,
        forward: Double
    ): Double {
        return rightVector.z * right + upVector.z * up + forwardVector.z * forward
    }

    private enum class State {
        IDLE, SUBTRACTING, EXTRACTING, RESTORING
    }

    private class CannonBlock(
        val blockNpc: NpcEntity,
        val isShouldAnimate: Boolean,
        val offset: Vector,
        val originalLocation: Location
    )

    companion object {

        fun clamp(`val`: Float, min: Float, max: Float): Float {
            return Math.max(min, Math.min(max, `val`))
        }
    }
}
