package net.craftventure.core.feature.shop.dto

import com.squareup.moshi.JsonClass
import org.bukkit.Location
import org.bukkit.entity.EntityType

@JsonClass(generateAdapter = true)
class ShopPresenterBalloonHolderDto(
    val id: String,
    val location: Location,
    val entityType: EntityType = EntityType.LEASH_HITCH,
)