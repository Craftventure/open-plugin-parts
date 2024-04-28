package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.animation.curve.LineairPointCurve
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.ktx.extension.orElse
import org.bukkit.Particle
import java.util.*

@JsonClass(generateAdapter = true)
data class KartBrakes(
    override val id: String = "default",
    override val displayName: String = "Default",
    override val extends: String? = null,
    val brakeParticleName: Optional<String>? = null,
    val brakeParticlesWhenIdle: Optional<Boolean>? = null,
    val brakeParticlesWhenDriving: Optional<Boolean>? = null,
    val hasHandbrake: Optional<Boolean>? = null,
    val brakeParticle: Particle? = brakeParticleName?.get()?.let { brakeParticleName ->
        Particle.values().firstOrNull { it.name.equals(brakeParticleName, ignoreCase = true) }
    },
    val brakeCurveForSpeed: Optional<LineairPointCurve>? = null,
    val handbrakingAngularDampingFactor: Optional<Double>? = null,
) : KartPart, NamedPart {
    override fun isValid() = brakeParticlesWhenIdle?.orElse() != null &&
            brakeParticlesWhenDriving?.orElse() != null &&
            hasHandbrake?.orElse() != null &&
            brakeCurveForSpeed?.orElse() != null &&
            handbrakingAngularDampingFactor?.orElse() != null
}