package net.craftventure.core.feature.shop.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class NpcFileShopKeeperDto(
    val id: String,
    val file: String,
    val offer: String? = null,
    val teamId: String? = null,
    val interactionRadius: Double? = null,
)