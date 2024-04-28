package net.craftventure.core.ride.shooter.config

import com.squareup.moshi.JsonClass
import org.bukkit.entity.EntityType

@JsonClass(generateAdapter = true)
data class ShooterConfig(
    val scenes: Map<String, ShooterSceneConfig>,
    val gunItem: String,
    val gunItemAlwaysInHand: Boolean,
    val gunCooldownSeconds: Double,
    val gravity: Double? = null,
    val shootSpeed: Double? = null,
    val bulletModel: Int = 1,
    val entityHitboxes: Map<EntityType, ShooterHitboxConfig> = emptyMap(),
)

