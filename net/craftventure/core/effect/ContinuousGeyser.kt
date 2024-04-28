package net.craftventure.core.effect

import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.CraftventureCore
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle


class ContinuousGeyser(
    override val name: String,
    x: Double,
    y: Double,
    z: Double,
    private val targetHeight: Double,
    private val pauseTime: Int,
    private val randomTime: Int
) : SimpleEffect, BackgroundService.Animatable {
    private var isPlaying = false
    private val location: Location
    private var randomizedTargetHeight: Double = 0.toDouble()
    private var nextExplode: Int = 0
    private var currentTime: Int = 0

    init {
        this.location = Location(Bukkit.getWorld("world"), x, y, z)
        this.nextExplode = pauseTime + CraftventureCore.getRandom().nextInt(randomTime)
    }

    override fun play() {
        if (!isPlaying) {
            isPlaying = true
            BackgroundService.add(this)
        }
    }

    override fun isPlaying(): Boolean {
        return isPlaying
    }

    override fun isStoppable(): Boolean {
        return true
    }

    override fun stop() {
        if (isPlaying) {
            isPlaying = false
            BackgroundService.remove(this)
        }
    }

    override fun onAnimationUpdate() {
        currentTime++

        var height = 0.0
        randomizedTargetHeight = 0.2
        if (currentTime > nextExplode) {
            randomizedTargetHeight = Math.cos((currentTime * nextExplode).toDouble()) * targetHeight
        }
        if (currentTime > nextExplode + 20 * 3) {
            currentTime = 0
            this.nextExplode = pauseTime + CraftventureCore.getRandom().nextInt(randomTime)
        }
        while (height < randomizedTargetHeight) {
            height += 0.1
            location.world?.spawnParticleX(
                Particle.WATER_SPLASH,
                location.x, location.y + height, location.z,
                5,
                (0.1 + height / 10f), 0.0, (0.1 + height / 10f)
            )
        }
    }
}
