package net.craftventure.core.serverevent

import net.craftventure.core.listener.ItemListener
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.database.type.ItemType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot


class OwnedItemUseEvent(
    val player: Player,
    val clickType: ItemListener.ClickType,
    val item: EquipmentManager.EquippedItemData,
    val hand: EquipmentSlot,
    val clickLocation: Location
) : Event(!Bukkit.isPrimaryThread()), Cancellable {
    private var cancelled: Boolean = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
