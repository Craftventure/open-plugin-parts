package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.animation.curve.LineairPointCurve
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.ktx.extension.orElse
import java.util.*

@JsonClass(generateAdapter = true)
data class KartSteer(
    override val id: String = "default",
    override val displayName: String = "Default",
    override val extends: String? = null,
    val forceCurveForSpeed: Optional<LineairPointCurve>? = null,
    val oversteerCurveForSpeed: Optional<LineairPointCurve>? = null,
) : KartPart, NamedPart {
    override fun isValid() = forceCurveForSpeed?.orElse() != null
}