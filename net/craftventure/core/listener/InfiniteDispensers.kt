package net.craftventure.core.listener

import net.craftventure.core.async.executeSync
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.inventory.InventoryHolder

class InfiniteDispensers : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDispense(event: BlockDispenseEvent) {
//        Logger.info("Block dispense")
        val inventoryHolder = event.block.state as? InventoryHolder ?: return
        try {
            executeSync(1) {
                inventoryHolder.inventory.addItem(event.item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}