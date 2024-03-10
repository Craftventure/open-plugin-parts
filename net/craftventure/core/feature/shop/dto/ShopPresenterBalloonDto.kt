package net.craftventure.core.feature.shop.dto

import com.squareup.moshi.JsonClass
import org.bukkit.Location

@JsonClass(generateAdapter = true)
class ShopPresenterBalloonDto(
    val balloonId: String,
    val location: Location?,
    val leashNpcId: String? = null,
    val maxLeashLength: Double? = null,
)