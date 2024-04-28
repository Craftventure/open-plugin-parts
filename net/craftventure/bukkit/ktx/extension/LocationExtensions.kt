package net.craftventure.bukkit.ktx.extension

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.util.Vector

@JvmOverloads
fun Location.safeHigherLocation(requiredHeight: Int = 2): Location {
    val world = world ?: return this
    val x = x
    val z = z
    for (i in this.blockY..world.maxHeight) {
        val block = world.getBlockAt(blockX, i, blockZ)
        if (block.isPassable) {
            val heightPassable = (0 until requiredHeight).all {
                world.getBlockAt(x.toInt(), i + it, z.toInt()).isPassable
            }
            if (heightPassable) {
                if (i < y) return this
                return Location(world, x, i.toDouble(), z, yaw, pitch)
            }
        }
    }
    return this
}

fun Location.set(x: Double, y: Double, z: Double): Location {
    this.x = x
    this.y = y
    this.z = z
    return this
}

fun Location.set(x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Location {
    this.x = x
    this.y = y
    this.z = z
    this.yaw = yaw
    this.pitch = pitch
    return this
}

fun Location.set(location: Vector) = set(location.x, location.y, location.z)
fun Location.set(location: Location) = set(location.x, location.y, location.z, location.yaw, location.pitch)

fun Location.nearbyPlayers(distance: Double, action: (Player) -> Unit) {
    val maxDistance = distance * distance
    Bukkit.getOnlinePlayers().forEach {
        if (it.location.distanceSquared(this) <= maxDistance) {
            action(it)
        }
    }
}

fun Location.rotateAroundY(pivot: Vector, angle: Double) = this.set(
    this.toVector()
        .subtract(pivot)
        .rotateAroundY(angle)
        .add(pivot)
)

fun Location.playSound(name: String, category: SoundCategory, volume: Float, pitch: Float) {
    world?.playSound(this, name, category, volume, pitch)
}