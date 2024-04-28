package net.craftventure.core.ride.shooter.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.shooter.hitaction.EntityHitAction

@JsonClass(generateAdapter = true)
data class ShooterSceneEntityConfig(
    val npc: String,
    val particles: List<ShooterParticleConfig>? = null,
    val repeat: Boolean = false,
    val firstSpawnAtSeconds: Double = 0.0,
    val stopOnSceneFinish: Boolean = true,
    val respawn: RespawnType = RespawnType.NEVER,
    val respawnInterval: Double? = null,
    val onHit: List<EntityHitAction.Data>,
    val hitboxConfig: ShooterHitboxConfig? = null,
) {
    companion object {
        enum class RespawnType {
            NEVER,
            INTERVAL_AFTER_HIT,
            INTERVAL_AFTER_SPAWN
        }
    }
}