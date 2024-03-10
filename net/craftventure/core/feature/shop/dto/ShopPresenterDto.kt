package net.craftventure.core.feature.shop.dto

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.CombinedArea

@JsonClass(generateAdapter = true)
class ShopPresenterDto(
    val shop: String,
    val interactionRadius: Double? = null,
    val visibilityArea: Array<Area.Json>,
    val items: Array<ShopItemDto>,
    val staticKeepers: Array<StaticShopKeeperDto> = emptyArray(),
    val npcKeepers: Array<NpcFileShopKeeperDto> = emptyArray(),
    val balloons: Array<ShopPresenterBalloonDto> = emptyArray(),
    val balloonHolders: Array<ShopPresenterBalloonHolderDto> = emptyArray(),
) {
    @Transient
    val visibilityAreaCombined = CombinedArea(*visibilityArea.map { it.create() }.toTypedArray())
}