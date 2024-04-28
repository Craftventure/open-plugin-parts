package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.extension.toOptional
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import java.util.*

@JsonClass(generateAdapter = true)
data class LegacyKartModelConfig(
    override val id: String = "default",
    override val extends: String? = null,
    var useHeadRotation: Optional<Boolean>? = null,
    var parentBone: Optional<String>? = null,
    var shouldPlayerBeInvisible: Optional<Boolean>? = null,
    val model: Optional<String>? = null,
    val position: Optional<Vector>? = null,
    var entityType: Optional<EntityType>? = EntityType.ARMOR_STAND.toOptional(),
    val sub: List<LegacyKartModelConfig>? = null
) : KartPart {
    override fun isValid() = model?.orElse() != null &&
            position?.orElse() != null &&
            entityType?.orElse() != null
}