package net.craftventure.core.feature.minigame

import org.bukkit.Location
import org.bukkit.World

data class SpawnLocation(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float = 0f
) {
    constructor(location: Location) : this(location.x, location.y, location.z, location.yaw, location.pitch)

    fun toLocation(world: World) = Location(world, x, y, z, yaw, pitch)
}