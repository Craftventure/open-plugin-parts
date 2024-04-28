package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.animation.curve.LineairPointCurve
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.ktx.extension.orElse
import java.util.*

@JsonClass(generateAdapter = true)
data class KartEngine(
    override val id: String = "default",
    override val displayName: String = "Default",
    override val extends: String? = null,
    val motorSoundUrl: Optional<String>? = null,
    val motorVolume: Optional<Double>? = null,
    val motorMinRate: Optional<Double>? = null,
    val motorBaseRate: Optional<Double>? = null,
    val motorMaxRate: Optional<Double>? = null,
    val speedForceCurveForSpeed: Optional<LineairPointCurve>? = null,
    val forwardSpeed: Optional<Double>? = null,
    val backwardSpeed: Optional<Double>? = null,
) : KartPart, NamedPart {
    override fun isValid() = speedForceCurveForSpeed?.orElse() != null &&
            forwardSpeed?.orElse() != null &&
            backwardSpeed?.orElse() != null
}