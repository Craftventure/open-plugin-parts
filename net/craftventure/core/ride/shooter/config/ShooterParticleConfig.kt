package net.craftventure.core.ride.shooter.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShooterParticleConfig(
    val file: String,
)