package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.animation.curve.LineairPointCurve
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.ktx.extension.orElse
import java.util.*

@JsonClass(generateAdapter = true)
data class KartPlaneLifter(
    override val id: String = "default",
    override val displayName: String = "Default",
    override val extends: String? = null,
    val minimumFlySpeed: Double,
    val maxStallVelocityLimit: Optional<Double>? = null,
    val stallSpeedVelocity: Optional<LineairPointCurve>? = null,
    val steerForceRotation: Optional<LineairPointCurve>? = null,

    val velocityTickDampening: Optional<Double>? = null,

    val flyVelocityUpSpeed: Optional<LineairPointCurve>? = null,
    val flyVelocityUpSpeedLimit: Optional<Double>? = null,

    val flyVelocityDownSpeed: Optional<LineairPointCurve>? = null,
    val flyVelocityDownSpeedLimit: Optional<Double>? = null,
) : KartPart, NamedPart {
    override fun isValid() = flyVelocityUpSpeed?.orElse() != null &&
            flyVelocityUpSpeedLimit?.orElse() != null &&
            flyVelocityDownSpeed?.orElse() != null &&
            flyVelocityDownSpeedLimit?.orElse() != null
}