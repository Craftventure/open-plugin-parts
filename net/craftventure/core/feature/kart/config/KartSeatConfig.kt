package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.ktx.extension.orElse
import org.bukkit.util.Vector
import java.util.*

@JsonClass(generateAdapter = true)
data class KartSeatConfig(
    @Transient
    override val id: String = "default",
    override val extends: String? = null,
    val parentBone: Optional<String>? = null,
    val shouldPlayerBeInvisible: Optional<Boolean>? = null,
    val compensateForEyeHeight: Optional<Boolean>? = null,
    val allowItems: Optional<Boolean>? = null,
    val position: Optional<Vector>? = null,
    val yaw: Optional<Float>? = null,
    val pitch: Optional<Float>? = null,
    val enterPermission: Optional<String>? = null,
) : KartPart {
    override fun isValid() = position?.orElse() != null
}