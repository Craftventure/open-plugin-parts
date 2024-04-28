package net.craftventure.core.feature.kart

import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import org.bukkit.entity.Player

interface KartOwner {
    fun isOwner(player: Player) = false

    fun handlePlayerWornItemUpdateEvent(event: PlayerEquippedItemsUpdateEvent) {}

    fun isKartEnabled(kart: Kart): Boolean = true
    fun canExit(kart: Kart): Boolean = true
    fun tryToExit(kart: Kart): Boolean = true
    fun onDestroyed(kart: Kart) {}
    fun shouldRespawn(kart: Kart): KartRespawnData? = null
    fun allowUserDestroying() = true
}