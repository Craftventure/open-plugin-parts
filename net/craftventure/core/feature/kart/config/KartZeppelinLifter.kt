package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.ktx.extension.orElse
import java.util.*

@JsonClass(generateAdapter = true)
data class KartZeppelinLifter(
    override val id: String = "default",
    override val displayName: String = "Default",
    override val extends: String? = null,
    val flyVelocityUpSpeed: Optional<Double>? = null,
    val flyVelocityUpSpeedLimit: Optional<Double>? = null,
    val flyVelocityDownSpeed: Optional<Double>? = null,
    val flyVelocityDownSpeedLimit: Optional<Double>? = null,
) : KartPart, NamedPart {
    override fun isValid() = flyVelocityUpSpeed?.orElse() != null &&
            flyVelocityUpSpeedLimit?.orElse() != null &&
            flyVelocityDownSpeed?.orElse() != null &&
            flyVelocityDownSpeedLimit?.orElse() != null
}