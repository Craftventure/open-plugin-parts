package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.core.extension.getItemId
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.impl.CraftventureMenu
import net.craftventure.core.inventory.impl.ProfileMenu
import net.craftventure.core.inventory.impl.RidesMenu
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent


class MenuItemListener : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
//        Logger.debug("PlayerInteractEvent ${event::class.java.name} ${player.openInventory.type}")
        if (player.openInventory.type != InventoryType.CREATIVE && player.openInventory.type != InventoryType.CRAFTING) return
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK ||
            event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK
        ) {
            when (event.item?.getItemId()) {
                CraftventureKeys.ID_ITEM_CRAFTVENTURE_MENU -> {
                    event.isCancelled = true
                    player.openMenu(CraftventureMenu.getInstance())
                }
                CraftventureKeys.ID_ITEM_ATTRACTIONS_MENU -> {
                    event.isCancelled = true
                    player.openMenu(RidesMenu(event.player))
                }
                CraftventureKeys.ID_ITEM_PROFILE_MENU -> {
                    event.isCancelled = true
                    player.openMenu(ProfileMenu(event.player))
                }
                CraftventureKeys.ID_ITEM_RESERVED -> event.isCancelled = true
            }
        }

    }
}
