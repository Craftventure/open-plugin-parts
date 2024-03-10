package net.craftventure.core.feature.shop

import net.craftventure.core.feature.shop.dto.ShopPresenterBalloonHolderDto
import net.craftventure.core.npc.NpcEntity


data class ShopBalloonHolder(
    val shopPresenter: ShopPresenter,
    val npc: NpcEntity,
    val config: ShopPresenterBalloonHolderDto,
)