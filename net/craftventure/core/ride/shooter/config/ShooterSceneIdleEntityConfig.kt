package net.craftventure.core.ride.shooter.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShooterSceneIdleEntityConfig(
    val npc: String,
    val despawnOnSceneStart: Boolean = true,
    val despawnOnAnimationEnd: Boolean = false,
)