package net.craftventure.core.ride.shooter.config

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area

@JsonClass(generateAdapter = true)
data class ShooterSceneConfig(
    val duration: Double,
    val keepPlayingSeconds: Double?,
    val gunCooldownSeconds: Double?,
    val area: Area.Json,
    val idleEntities: List<ShooterSceneIdleEntityConfig>? = null,
    val entities: List<ShooterSceneEntityConfig>,
    val idleParticles: List<ShooterParticleConfig>? = null,
    val particles: List<ShooterParticleConfig>? = null,
)