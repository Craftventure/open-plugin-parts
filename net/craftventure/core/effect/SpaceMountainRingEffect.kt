package net.craftventure.core.effect

import net.craftventure.bukkit.ktx.extension.colorFromHex
import net.craftventure.core.ride.trackedride.TrackSegment
import net.craftventure.core.ride.trackedride.TrackedRideManager
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.util.Vector

class SpaceMountainRingEffect : BaseEffect("smring") {
    private val vector = Vector(0, 0, 0)
    private var trackSegment: TrackSegment? = null
    private var currentDistance = 0.0
    private var location: Location? = null
    private var angleOffset = 0.0

    override fun reset() {
        super.reset()
        currentDistance = 0.0
    }

    private fun setLampOn(location: Location, on: Boolean) {
        location.block.type = if (on) Material.GLOWSTONE else Material.BARRIER
    }

    override fun onStarted() {
        super.onStarted()

        setLampOn(Location(Bukkit.getWorld("world"), 260.5, 27.0, -782.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 260.5, 27.0, -780.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 267.5, 27.0, -785.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 268.5, 27.0, -784.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 275.5, 27.0, -787.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 276.5, 27.0, -786.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 283.5, 27.0, -785.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 283.5, 27.0, -787.5), true)
        setLampOn(Location(Bukkit.getWorld("world"), 289.5, 29.0, -785.5), true)
    }

    override fun onStopped() {
        super.onStopped()

        setLampOn(Location(Bukkit.getWorld("world"), 260.5, 27.0, -782.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 260.5, 27.0, -780.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 267.5, 27.0, -785.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 268.5, 27.0, -784.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 275.5, 27.0, -787.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 276.5, 27.0, -786.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 283.5, 27.0, -785.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 283.5, 27.0, -787.5), false)
        setLampOn(Location(Bukkit.getWorld("world"), 289.5, 29.0, -785.5), false)
    }

    override fun update(tick: Int) {
        if (trackSegment == null) {
            trackSegment = TrackedRideManager.getTrackedRide("spacemountain")!!.getSegmentById("track5")
        }
        if (location == null) {
            location = Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0)
        }
        if (trackSegment == null)
            return

        for (j in 0 until RING_COUNT) {
            currentDistance += DISTANCE_TOTAL / RING_COUNT.toDouble()

            while (currentDistance < DISTANCE_MIN)
                currentDistance += DISTANCE_TOTAL.toDouble()
            while (currentDistance > DISTANCE_MAX)
                currentDistance -= DISTANCE_TOTAL.toDouble()

            trackSegment!!.getPosition(currentDistance, vector)

            location!!.x = vector.x
            location!!.y = vector.y
            location!!.z = vector.z

            val offset = Math.PI * 2.0 / PARTICLE_COUNT.toDouble()
            for (i in 0 until PARTICLE_COUNT) {
                val yOffset = Math.cos(angleOffset + i * offset) * 4
                val zOffset = Math.sin(angleOffset + i * offset) * 4

                location!!.world?.spawnParticleX(
                    Particle.REDSTONE,
                    location!!.x, location!!.y + yOffset, location!!.z + zOffset,
                    5,
                    0.1, 0.1, 0.1,
                    data = Particle.DustOptions(colorFromHex("#790909"), 3f)
                )
            }
        }

        currentDistance -= 1.0
        angleOffset -= 0.1

        if (tick > 20 * 8) {
            stop()
        }
    }

    companion object {

        private val PARTICLE_COUNT = 8
        private val RING_COUNT = 3

        private val DISTANCE_MAX = 210
        private val DISTANCE_MIN = 171
        private val DISTANCE_TOTAL = DISTANCE_MAX - DISTANCE_MIN
    }
}
