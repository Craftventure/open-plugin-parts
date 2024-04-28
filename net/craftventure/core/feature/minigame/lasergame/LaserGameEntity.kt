package net.craftventure.core.feature.minigame.lasergame

import net.craftventure.bukkit.ktx.extension.setYawPitchDegrees
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Entity
import org.bukkit.util.Vector

interface LaserGameEntity {
    val currentLocation: Location
    val mainHitTarget: Location
        get() = currentLocation
    val name: String
    val displayName: String
        get() = name
    var owner: LaserGameEntity?

    val invincible: Boolean
    val isDead: Boolean

    var kills: Int

    fun applyVelocity(velocity: Vector) {}

    fun awardKill() {
        kills++
    }

    fun isEnemy(source: LaserGameEntity): Boolean {
        if (source === this) return false
        if (owner === source) return false
        if (source.owner === this) return false
        return true
    }

    fun canBeHit(): Boolean

    /**
     * @return true if the damage was handled
     */
    fun hitBy(source: LaserGameEntity, damage: Int): HitResult

    /**
     * @return HitType if location will cause a hit, null otherwise
     */
    fun isHit(location: Location): HitType?

    fun isPartOfTarget(entity: Entity): Boolean

    private fun debugRadius(location: Location, radius: Double, color: Color) {
        val vector = Vector()
        for (yaw in 0..360 step 45) {
            for (pitch in 0..360 step 45) {
                vector.setYawPitchDegrees(yaw.toDouble(), pitch.toDouble())
                location.clone().add(vector.multiply(radius))
                    .spawnParticleX(
                        particle = Particle.REDSTONE,
                        data = Particle.DustOptions(color, 0.5f)
                    )
            }
        }
    }

    enum class HitResult {
        DAMAGED,
        DESTROYED,
        IGNORED
    }

    data class DamageType(
        val icon: String
    ) {
        companion object {
            val fallback = DamageType("x")
            val gun = DamageType("\uE00A")
            val shotgun = DamageType("\uE00A")
            val sniper = DamageType("\uE00B")
            val turret = DamageType("\uE00C")
            val headshot = DamageType("\uE00D")
        }
    }

    enum class HitType(
        val priority: Int,
        val dealsDamage: Boolean = true,
        val critical: Boolean = false
    ) {
        NEAR_HIT(priority = 1, dealsDamage = false),
        NORMAL(priority = 2),
        CRITICAL(priority = 3, critical = true),
        HEADSHOT(priority = 4, critical = true)
    }

    val rootOwner: LaserGameEntity?
        get() {
            val owner = this.owner
            return owner?.rootOwner ?: owner
        }
}