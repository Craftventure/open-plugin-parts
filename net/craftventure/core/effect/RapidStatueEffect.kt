package net.craftventure.core.effect

import net.craftventure.core.CraftventureCore
import net.craftventure.core.utils.MathUtil
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.Vector


class RapidStatueEffect(
    override val name: String,
    private val baseLocation: Location,
    private val yaw: Double,
    private val pitch: Double
) : SimpleEffect, Runnable {
    private var isPlaying = false
    private var task = -1
    private var timeOut = (CraftventureCore.getRandom().nextInt(20 * 3) + 50).toLong()
    private var currentTimeout: Long = 0

    private var tick = 0
    private var vec: Vector? = null
    private var effectLoc: Location? = null
    private var hit = false

    override fun play() {
        if (!isPlaying) {
            reset()
            isPlaying = true
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
            reset()
        }
    }

    private fun reset() {
        //        Logger.consoleAndIngame("Resetted dem dragon");
        tick = 0
        hit = false

        timeOut = (CraftventureCore.getRandom().nextInt(20 * 3) + 50).toLong()
        currentTimeout = 0
        resetLocations()
    }

    private fun resetLocations() {
        effectLoc = baseLocation.clone()
        vec = MathUtil.setYawPitchDegrees(Vector(), yaw, pitch)
    }

    override fun run() {
        currentTimeout++
        if (currentTimeout < timeOut)
            return
        hit = tick > 20 * 3
        if (hit) {
            stop()
        } else {
            resetLocations()
            for (i in 0 until tick) {
                vec!!.x = vec!!.x * 0.93
                vec!!.z = vec!!.z * 0.93
                vec!!.y = vec!!.y - 0.02 * Math.pow(1.1, i.toDouble())

                effectLoc!!.add(vec!!)
                if (effectLoc!!.y < 35)
                    break

                effectLoc!!.spawnParticleX(
                    Particle.WATER_SPLASH,
                    30,
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
