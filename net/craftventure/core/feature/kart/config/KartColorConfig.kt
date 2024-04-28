package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart
import net.craftventure.core.feature.kart.NamedPart
import net.craftventure.core.ktx.extension.orElse
import java.util.*

@JsonClass(generateAdapter = true)
data class KartColorConfig(
    override val id: String,
    override val displayName: String,
    override val extends: String? = null,
    val defaultColor: Optional<String>? = null,
) : KartPart, NamedPart {
    override fun isValid() = defaultColor?.orElse() != null
}