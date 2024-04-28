package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.util.CraftventureKeys.shopId
import net.craftventure.core.inventory.impl.ShopOfferMenu
import net.craftventure.database.bukkit.listener.ShopCacheListener
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent


class EntityPersistentDataListener : Listener {
    @EventHandler
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        if (player.gameMode == GameMode.CREATIVE && player.isSneaking) return
        val shopId = event.entity.persistentDataContainer.shopId ?: return
        val shop = ShopCacheListener.cached(shopId) ?: return
        event.isCancelled = true
        ShopOfferMenu(player, shop).openAsMenu(player)
    }

    @EventHandler
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE && player.isSneaking) return
        val shopId = event.rightClicked.persistentDataContainer.shopId ?: return
        val shop = ShopCacheListener.cached(shopId) ?: return
        event.isCancelled = true
        ShopOfferMenu(player, shop).openAsMenu(player)
    }
}
