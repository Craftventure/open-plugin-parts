package net.craftventure.core.utils

import com.squareup.moshi.JsonClass
import net.minecraft.world.phys.AABB
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.util.Vector


@JsonClass(generateAdapter = true)
data class BoundingBox(
    var xMin: Double = 0.0,
    var yMin: Double = 0.0,
    var zMin: Double = 0.0,
    var xMax: Double = 0.0,
    var yMax: Double = 0.0,
    var zMax: Double = 0.0
) {
    fun moveBy(x: Double, y: Double, z: Double) {
        this.xMin += x
        this.yMin += y
        this.zMin += z
        this.xMax += x
        this.yMax += y
        this.zMax += z
    }

    fun setFrom(other: BoundingBox) {
        this.xMin = other.xMin
        this.yMin = other.yMin
        this.zMin = other.zMin
        this.xMax = other.xMax
        this.yMax = other.yMax
        this.zMax = other.zMax
    }

    fun intersectsWith(
        thisOffsetX: Double,
        thisOffsetY: Double,
        thisOffsetZ: Double,
        otherOffsetX: Double,
        otherOffsetY: Double,
        otherOffsetZ: Double,
        other: BoundingBox
    ): Boolean {
        if (otherOffsetX + other.xMax <= thisOffsetX + xMin || otherOffsetX + other.xMin >= thisOffsetX + xMax) {
//            Logger.console("X failed")
            return false
        }
        if (otherOffsetY + other.yMax <= thisOffsetY + yMin || otherOffsetY + other.yMin >= thisOffsetY + yMax) {
//            Logger.console("Y failed")
            return false
        }
        if (otherOffsetZ + other.zMax <= thisOffsetZ + zMin || otherOffsetZ + other.zMin >= thisOffsetZ + zMax) {
//            Logger.console("Z failed")
            return false
        }
        return true
    }

    fun xIntersectsWithXOf(thisOffsetX: Double, otherOffsetX: Double, other: BoundingBox): Boolean {
        if (otherOffsetX + other.xMax <= thisOffsetX + xMin || otherOffsetX + other.xMin >= thisOffsetX + xMax) {
//            Logger.console("X failed")
            return false
        }
        return true
    }

    fun yIntersectsWithYOf(thisOffsetY: Double, otherOffsetY: Double, other: BoundingBox): Boolean {
        if (otherOffsetY + other.yMax <= thisOffsetY + yMin || otherOffsetY + other.yMin >= thisOffsetY + yMax) {
//            Logger.console("Y failed")
            return false
        }
        return true
    }

    fun zIntersectsWithZOf(thisOffsetZ: Double, otherOffsetZ: Double, other: BoundingBox): Boolean {
        if (otherOffsetZ + other.zMax <= thisOffsetZ + zMin || otherOffsetZ + other.zMin >= thisOffsetZ + zMax) {
//            Logger.console("Z failed")
            return false
        }
        return true
    }

    fun intersectsWith(thisOffset: Vector, otherOffset: Vector, other: BoundingBox): Boolean {
        if (otherOffset.x + other.xMax <= thisOffset.x + xMin || otherOffset.x + other.xMin >= thisOffset.x + xMax) {
//            Logger.console("X failed")
            return false
        }
        if (otherOffset.y + other.yMax <= thisOffset.y + yMin || otherOffset.y + other.yMin >= thisOffset.y + yMax) {
//            Logger.console("Y failed")
            return false
        }
        if (otherOffset.z + other.zMax <= thisOffset.z + zMin || otherOffset.z + other.zMin >= thisOffset.z + zMax) {
//            Logger.console("Z failed")
            return false
        }
        return true
    }

    fun set(axisAlignedBB: AABB): BoundingBox {
        xMin = axisAlignedBB.minX
        yMin = axisAlignedBB.minY
        zMin = axisAlignedBB.minZ
        xMax = axisAlignedBB.maxX
        yMax = axisAlignedBB.maxY
        zMax = axisAlignedBB.maxZ
        return this
    }

    fun set(boundingBox: org.bukkit.util.BoundingBox): BoundingBox {
        xMin = boundingBox.minX
        yMin = boundingBox.minY
        zMin = boundingBox.minZ
        xMax = boundingBox.maxX
        yMax = boundingBox.maxY
        zMax = boundingBox.maxZ
        return this
    }

    fun debug(location: Location) {
        debug(location.world!!, location.toVector())
    }

    fun debug(world: World, location: Vector) {
        debug(world, location.x, location.y, location.z)
    }

    fun debug(world: World, x: Double, y: Double, z: Double, particle: Particle = Particle.END_ROD) {
        world.spawnParticleX(
            particle,
            x + xMin,
            y + yMin,
            z + zMin,
            0
        )
        world.spawnParticleX(
            particle,
            x + xMax,
            y + yMin,
            z + zMin,
            0
        )
        world.spawnParticleX(
            particle,
            x + xMin,
            y + yMin,
            z + zMax,
            0
        )
        world.spawnParticleX(
            particle,
            x + xMax,
            y + yMin,
            z + zMax,
            0
        )

        world.spawnParticleX(
            particle,
            x + xMin,
            y + yMax,
            z + zMin,
            0
        )
        world.spawnParticleX(
            particle,
            x + xMax,
            y + yMax,
            z + zMin,
            0
        )
        world.spawnParticleX(
            particle,
            x + xMin,
            y + yMax,
            z + zMax,
            0
        )
        world.spawnParticleX(
            particle,
            x + xMax,
            y + yMax,
            z + zMax,
            0
        )
    }
}

fun org.bukkit.util.BoundingBox.debug(world: World, particle: Particle = Particle.END_ROD, data: Any? = null) {
    world.spawnParticleX(
        particle,
        minX,
        minY,
        minZ,
        0,
        data = data
    )
    world.spawnParticleX(
        particle,
        maxX,
        minY,
        minZ,
        0,
        data = data
    )
    world.spawnParticleX(
        particle,
        minX,
        minY,
        maxZ,
        0,
        data = data
    )
    world.spawnParticleX(
        particle,
        maxX,
        minY,
        maxZ,
        0,
        data = data
    )

    world.spawnParticleX(
        particle,
        minX,
        maxY,
        minZ,
        0,
        data = data
    )
    world.spawnParticleX(
        particle,
        maxX,
        maxY,
        minZ,
        0,
        data = data
    )
    world.spawnParticleX(
        particle,
        minX,
        maxY,
        maxZ,
        0,
        data = data
    )
    world.spawnParticleX(
        particle,
        maxX,
        maxY,
        maxZ,
        0,
        data = data
    )
}