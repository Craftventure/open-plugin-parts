package net.craftventure.core.feature.shop

import net.craftventure.core.feature.balloon.BalloonManager
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.types.Balloon
import net.craftventure.core.feature.shop.dto.ShopPresenterBalloonDto


data class ShopBalloonDisplay(
    val shopPresenter: ShopPresenter,
    val holderCreator: () -> BalloonHolder,
    val balloon: Balloon,
    val config: ShopPresenterBalloonDto,
) {
    private var holder: BalloonHolder? = null

    fun create() {
        destroy()
        holder = holderCreator()
        BalloonManager.create(holder!!, balloon)
    }

    fun destroy() {
        holder?.let { BalloonManager.remove(it) }
    }
}