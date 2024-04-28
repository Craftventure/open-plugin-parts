package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.ktx.extension.orElse
import java.util.*

@JsonClass(generateAdapter = true)
data class KartHandling(
    override val id: String = "default",
    override val displayName: String = "Default",
    override val extends: String? = null,
    val maxClimb: Optional<Double>? = null,
    val mass: Optional<Double>? = null,
    val axisOffset: Optional<Double>? = null,
    val fallbackFriction: Optional<Double>? = null,
    val airSteerInfluence: Optional<Double>? = null,
    val allowUnderwater: Optional<Boolean>? = null,
    val physicsControllerId: Optional<String>? = null,
    val forceOffset: Optional<Double>? = null,
) : KartPart, NamedPart {
    override fun isValid() = maxClimb?.orElse() != null &&
            mass?.orElse() != null &&
            axisOffset?.orElse() != null &&
            allowUnderwater?.orElse() != null &&
            physicsControllerId?.orElse() != null &&
            airSteerInfluence?.orElse() != null
}