package net.craftventure.core.feature.kart

import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import org.bukkit.entity.Player

class DefaultPlayerKartOwner(val player: Player) : KartOwner {
    override fun isOwner(player: Player): Boolean = player === this.player

    override fun handlePlayerWornItemUpdateEvent(event: PlayerEquippedItemsUpdateEvent) {
//        event.wornData.helmetItem = null
        event.appliedEquippedItems.eventItem = null
        event.appliedEquippedItems.consumptionItem = null
        event.appliedEquippedItems.balloonItem = null
        event.appliedEquippedItems.offhandItem = null
        event.appliedEquippedItems.weaponItem = null
        event.appliedEquippedItems.shoulderPetLeft = null
        event.appliedEquippedItems.shoulderPetRight = null
    }
}