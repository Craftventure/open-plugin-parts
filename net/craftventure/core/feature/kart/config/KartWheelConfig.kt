package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.ktx.extension.orElse
import org.bukkit.util.Vector
import java.util.*

@JsonClass(generateAdapter = true)
data class KartWheelConfig(
    @Transient
    override val id: String = "default",
    override val extends: String? = null,
    val parentBone: Optional<String>? = null,
    val model: Optional<String>? = null,
    val position: Optional<Vector>? = null,
    val isLeftSide: Optional<Boolean>? = null,
    val hasBrakes: Optional<Boolean>? = null,
    val isSteered: Optional<Boolean>? = null,
    val steerAngle: Optional<Double>? = null,
    val radius: Optional<Double>? = null,
    val forceCustomParticle: Optional<Boolean>? = null,
) : KartPart {
    override fun isValid() = position?.orElse() != null &&
            isLeftSide?.orElse() != null &&
            forceCustomParticle?.orElse() != null
}