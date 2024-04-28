package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.bukkit.ktx.util.CraftventureKeys.shopId
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.extension.isSign
import net.craftventure.core.inventory.impl.ShopOfferMenu
import net.craftventure.database.bukkit.listener.ShopCacheListener
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import java.io.IOException


class TileStateListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    @Throws(IOException::class)
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
//        if (event.player.isSneaking) return
        if (player.gameMode == GameMode.CREATIVE && (player.isSneaking || event.action == Action.LEFT_CLICK_BLOCK)) return
        val type = player.inventory.itemInMainHand.type
//        Logger.debug("hand=$type ${type.isSign()}")
        val clickedBlock = event.clickedBlock ?: return
        val state = clickedBlock.state
//        Logger.debug("Clicked block ${clickedBlock.type} state=$state")
        if (state is PersistentDataHolder) {
            state.persistentDataContainer.get(CraftventureKeys.TILE_STATE_BOOK, PersistentDataType.BYTE_ARRAY)
                ?.let { bookData ->
                    try {
                        val itemStack = ItemStack.deserializeBytes(bookData)
                        player.openBook(itemStack)
                    } catch (e: Exception) {
                        player.sendMessage(CVTextColor.serverError + "Failed to open book")
                    }
                    event.isCancelled = true
                }

            if (player.gameMode != GameMode.CREATIVE || !type.isSign())
                state.persistentDataContainer.shopId?.let { shopId ->
                    event.isCancelled = true
                    val shop = ShopCacheListener.cached(shopId)
                    if (shop != null) {
                        ShopOfferMenu(player, shop).openAsMenu(player)
                    }
                }
        }
        return
    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
//        if (event.rightClicked is Player ) {
//            val damager = event.player
//            val target = event.rightClicked as Player
//            if (damager.isCrew()) {
//                ProfileMenu(damager, target.uniqueId).open(damager)
//            }
//        }
//    }
}
