package net.craftventure.core.effect

import net.craftventure.core.CraftventureCore
import net.craftventure.core.utils.MathUtil
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.util.Vector


class RapidSnakeWaterEffect(override val name: String) : SimpleEffect, Runnable {
    private var isPlaying = false
    private var task = -1
    private val torchLocation = Location(Bukkit.getWorld("world"), -232.0, 46.0, -722.0)

    private var tick = 0
    private var vec: Vector? = null
    private var effectLoc: Location? = null
    private var hit = false

    override fun play() {
        if (!isPlaying) {
            reset()
            isPlaying = true
            torchLocation.block.type = Material.REDSTONE_TORCH
            task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this, 1L, 1L)
        }
    }

    override fun isPlaying(): Boolean {
        return isPlaying
    }

    override fun isStoppable(): Boolean {
        return false
    }

    override fun stop() {
        if (isPlaying) {
            isPlaying = false
            Bukkit.getScheduler().cancelTask(task)
            torchLocation.block.type = Material.AIR
            reset()
        }
    }

    private fun reset() {
        //        Logger.consoleAndIngame("Resetted dem dragon");
        tick = 0
        hit = false
        resetLocations()
    }

    private fun resetLocations() {
        effectLoc = Location(Bukkit.getWorld("world"), -230.5, 44.5, -721.5)
        vec = MathUtil.setYawPitchDegrees(Vector(), -90.0, 0.0)
    }

    override fun run() {
        hit = tick > 20 * 3
        if (hit) {
            stop()
        } else {
            resetLocations()
            for (i in 0 until tick) {
                vec!!.x = vec!!.x * 0.96
                vec!!.z = vec!!.z * 0.96
                vec!!.y = vec!!.y - 0.02 * Math.pow(1.1, i.toDouble())

                effectLoc!!.add(vec!!)
                if (effectLoc!!.y < 40)
                    break

                effectLoc!!.spawnParticleX(
                    Particle.WATER_SPLASH,
                    60,
                    0.25,
                    0.25,
                    0.25,
                    0.0,
                    null,
                    false,
                    30.0
                )
            }
            tick++
        }
    }
}
