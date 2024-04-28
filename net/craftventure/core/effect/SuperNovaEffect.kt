package net.craftventure.core.effect

import net.craftventure.core.utils.MathUtil
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.Vector


class SuperNovaEffect : BaseEffect("smsupernova") {

    private var forwardVector = Vector(0, 0, 0)
    private val rightVector = Vector(0, -1, 0)
    private val upVector = Vector(0, -1, 0)
    private val bankingVector = Vector(-1, 0, 0)
    private val centerLocation = Location(Bukkit.getWorld("world"), 278.5, 61.0, -772.5)

    init {

        val banking = 0.0
        val workYaw = Math.toRadians(316.0)//trackYawRadian + (Math.PI * 0.5);
        val workPitch = Math.toRadians(-70.0)//trackPitchRadian + (Math.PI * 0.5);

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
    }

    override fun update(tick: Int) {
        val RING_PARTICLE_COUNT = 20.0
        centerLocation.spawnParticleX(
            Particle.FLAME,
            4,
            clamp(tick / (5 * 20.0), 0.0, 0.3),
            clamp(tick / (5 * 20.0), 0.0, 0.3),
            clamp(tick / (5 * 20.0), 0.0, 0.3)
        )

        if (tick > 20 * 3 && tick < 20 * 7) {
            val percentage = clamp((tick - 20 * 3.0), 0.0, (20 * 3.0)) / (20 * 2.0)
            var i = 0
            while (i < RING_PARTICLE_COUNT) {
                val angle = i / RING_PARTICLE_COUNT
                val rightOffset = 3.0 * Math.cos(angle * Math.PI * 2.0) * percentage
                val upOffset = 3.0 * Math.sin(angle * Math.PI * 2.0) * percentage
                centerLocation.world?.spawnParticleX(
                    Particle.FLAME,
                    centerLocation.x - calculateX(rightVector, upVector, forwardVector, rightOffset, 0.0, upOffset),
                    centerLocation.y - calculateY(rightVector, upVector, forwardVector, rightOffset, 0.0, upOffset),
                    centerLocation.z - calculateZ(rightVector, upVector, forwardVector, rightOffset, 0.0, upOffset),
                    1,
                    0.1 * percentage,
                    0.1 * percentage,
                    0.1 * percentage
                )
                i++
            }

            if ((tick < 20 * 4) and (tick % 3 == 0)) {
                centerLocation.world?.spawnParticleX(
                    Particle.EXPLOSION_LARGE,
                    centerLocation.x,
                    centerLocation.y,
                    centerLocation.z,
                    1,
                    0.3,
                    0.3,
                    0.3
                )
            }
        }

        if (tick > 20 * 6 && tick < 20 * 10) {
            val percentage = clamp((tick - 20 * 6.0), 0.0, (20 * 3.0)) / (20 * 2.0)
            var i = 0
            while (i < RING_PARTICLE_COUNT) {
                val angle = i / RING_PARTICLE_COUNT
                val rightOffset = 3.0 * Math.cos(angle * Math.PI * 2.0) * percentage
                val upOffset = 3.0 * Math.sin(angle * Math.PI * 2.0) * percentage
                centerLocation.world?.spawnParticleX(
                    Particle.FLAME,
                    centerLocation.x - calculateX(
                        rightVector,
                        upVector,
                        forwardVector,
                        rightOffset,
                        upOffset,
                        upOffset
                    ),
                    centerLocation.y - calculateY(
                        rightVector,
                        upVector,
                        forwardVector,
                        rightOffset,
                        upOffset,
                        upOffset
                    ),
                    centerLocation.z - calculateZ(
                        rightVector,
                        upVector,
                        forwardVector,
                        rightOffset,
                        upOffset,
                        upOffset
                    ),
                    1,
                    0.1 * percentage,
                    0.1 * percentage,
                    0.1 * percentage
                )
                i++
            }

            if ((tick < 20 * 7) and (tick % 3 == 0)) {
                centerLocation.world?.spawnParticleX(
                    Particle.EXPLOSION_LARGE,
                    centerLocation.x,
                    centerLocation.y,
                    centerLocation.z,
                    1,
                    0.3,
                    0.3,
                    0.3
                )
            }
        }

        if (tick > 20 * 9 && tick < 20 * 13) {
            val percentage = clamp((tick - 20 * 9.0), 0.0, (20 * 3.0)) / (20 * 2.2)
            var i = 0
            while (i < RING_PARTICLE_COUNT) {
                val angle = i / RING_PARTICLE_COUNT
                val rightOffset = 3.5 * Math.cos(angle * Math.PI * 2.0) * percentage
                val upOffset = 3.5 * Math.sin(angle * Math.PI * 2.0) * percentage
                centerLocation.world?.spawnParticleX(
                    Particle.FLAME,
                    centerLocation.x - calculateX(rightVector, upVector, forwardVector, rightOffset, upOffset, 0.0),
                    centerLocation.y - calculateY(rightVector, upVector, forwardVector, rightOffset, upOffset, 0.0),
                    centerLocation.z - calculateZ(rightVector, upVector, forwardVector, rightOffset, upOffset, 0.0),
                    1,
                    0.1 * percentage,
                    0.1 * percentage,
                    0.1 * percentage
                )
                centerLocation.world?.spawnParticleX(
                    Particle.END_ROD,
                    centerLocation.x - calculateX(
                        rightVector,
                        upVector,
                        forwardVector,
                        rightOffset,
                        upOffset,
                        upOffset
                    ),
                    centerLocation.y - calculateY(
                        rightVector,
                        upVector,
                        forwardVector,
                        rightOffset,
                        upOffset,
                        upOffset
                    ),
                    centerLocation.z - calculateZ(
                        rightVector,
                        upVector,
                        forwardVector,
                        rightOffset,
                        upOffset,
                        upOffset
                    ),
                    1,
                    0.1 * percentage,
                    0.1 * percentage,
                    0.1 * percentage
                )
                i++
            }

            if ((tick < 20 * 10) and (tick % 3 == 0)) {
                centerLocation.world?.spawnParticleX(
                    Particle.EXPLOSION_LARGE,
                    centerLocation.x,
                    centerLocation.y,
                    centerLocation.z,
                    1,
                    0.3,
                    0.3,
                    0.3
                )
            }
        }

        if (tick > 20 * 6) {
            if (tick % 5 == 0) {
                for (i in 0..4) {
                    val angle = i / 5.0
                    val rightOffset = 1.3 * Math.cos(angle * Math.PI * 2.0)
                    val upOffset = 1.3 * Math.sin(angle * Math.PI * 2.0)
                    centerLocation.world?.spawnParticleX(
                        Particle.LAVA,
                        centerLocation.x - calculateX(rightVector, upVector, forwardVector, rightOffset, upOffset, 0.0),
                        centerLocation.y - calculateY(rightVector, upVector, forwardVector, rightOffset, upOffset, 0.0),
                        centerLocation.z - calculateZ(rightVector, upVector, forwardVector, rightOffset, upOffset, 0.0),
                        1,
                        0.3,
                        0.3,
                        0.3
                    )
                }
            }
        }

        if (tick > 20 * 9.5) {
            val percentage = clamp(tick - 20 * 9.0, 0.0, 20 * 3.0) / (20 * 2.2)
            var offset = (tick - 20 * 6) * 0.1
            offset = Math.min(offset, 3.0)
            centerLocation.spawnParticleX(
                Particle.SWEEP_ATTACK,
                4,
                offset * percentage,
                offset * percentage,
                offset * percentage
            )
        }

        if (tick > 20 * 15) {
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

    companion object {

        fun clamp(`val`: Double, min: Double, max: Double): Double {
            return Math.max(min, Math.min(max, `val`))
        }
    }
}
