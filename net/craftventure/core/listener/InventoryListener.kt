package net.craftventure.core.listener

import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.InventoryHolder

class InventoryListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val meta = InventoryTrackingMeta.get(player)
        meta?.onInventoryOpened()

        (event.inventory.holder as? ManagedInventoryHolder)?.onOpened(player)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onInventoryClosed(event: InventoryCloseEvent) {
//        logcat { "Inv closed event" }
//        if (event.reason == InventoryCloseEvent.Reason.OPEN_NEW) return
        val player = event.player as? Player ?: return
        val meta = InventoryTrackingMeta.get(player)
        meta?.onInventoryClosed(event.reason)

        (event.inventory.holder as? ManagedInventoryHolder)?.onClosed(player)
    }

    interface ManagedInventoryHolder : InventoryHolder {
        fun onOpened(player: Player)
        fun onClosed(player: Player)

        val menu: InventoryMenu
    }
}