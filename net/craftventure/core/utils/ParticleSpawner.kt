package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.extension.nearbyPlayers
import net.craftventure.core.utils.ParticleSpawner.DEFAULT_RANGE
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.Vector

object ParticleSpawner {
    const val DEFAULT_RANGE = 25.0
    fun spawnParticle(
        world: World,
        particle: Particle,
        x: Double,
        y: Double,
        z: Double,
        count: Int = 1,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0,
        extra: Double = 0.0,
        data: Any? = null,
        longDistance: Boolean = false,
        range: Double? = DEFAULT_RANGE,
        players: Set<Player>? = null,
        exclude: Set<Player>? = null,
    ) {
        // 1.13 fix...
        val usePacket = range != null || players != null || exclude != null
        if (!usePacket) {
            world.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data, longDistance)
        } else {
            if (players != null) {
                for (player in players) {
                    if (exclude != null && player in exclude) return
                    // TODO: Handle longDistance correctly
                    player.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data)
                }
            } else if (range != null) {
                Location(world, x, y, z).nearbyPlayers(range) { player ->
                    if (exclude != null && player in exclude) return@nearbyPlayers
                    // TODO: Handle longDistance correctly
                    player.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data)
                }
            }
        }
    }
}

@JvmOverloads
fun World.spawnParticleX(
    particle: Particle,
    x: Double,
    y: Double,
    z: Double,
    count: Int = 1,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    offsetZ: Double = 0.0,
    extra: Double = 0.0,
    data: Any? = null,
    longDistance: Boolean = false,
    range: Double? = DEFAULT_RANGE,
    players: Set<Player>? = null,
    exclude: Set<Player>? = null,
) {
    ParticleSpawner.spawnParticle(
        world = this,
        particle = particle,
        x = x, y = y, z = z,
        count = count,
        offsetX = offsetX, offsetY = offsetY, offsetZ = offsetZ,
        extra = extra,
        data = data,
        longDistance = longDistance,
        range = range,
        players = players,
        exclude = exclude,
    )
}

@JvmOverloads
fun Location.spawnParticleX(
    particle: Particle,
    count: Int = 1,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    offsetZ: Double = 0.0,
    extra: Double = 0.0,
    data: Any? = null,
    longDistance: Boolean = false,
    range: Double? = DEFAULT_RANGE,
    players: Set<Player>? = null,
    exclude: Set<Player>? = null,
) {
    world?.spawnParticleX(
        particle = particle,
        x = x, y = y, z = z,
        count = count,
        offsetX = offsetX,
        offsetY = offsetY,
        offsetZ = offsetZ,
        extra = extra,
        data = data,
        longDistance = longDistance,
        range = range,
        players = players,
        exclude = exclude,
    )
}

@JvmOverloads
fun Vector.spawnParticleX(
    world: World,
    particle: Particle,
    count: Int = 1,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    offsetZ: Double = 0.0,
    extra: Double = 0.0,
    data: Any? = null,
    longDistance: Boolean = false,
    range: Double? = DEFAULT_RANGE,
    players: Set<Player>? = null,
    exclude: Set<Player>? = null,
) {
    world.spawnParticleX(
        particle = particle,
        x = x, y = y, z = z,
        count = count,
        offsetX = offsetX,
        offsetY = offsetY,
        offsetZ = offsetZ,
        extra = extra,
        data = data,
        longDistance = longDistance,
        range = range,
        players = players,
        exclude = exclude,
    )
}