package net.craftventure.core.serverevent

import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.EquippedItemsMeta
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class PlayerEquippedItemsUpdateEvent(
    val player: Player,
    val meta: EquippedItemsMeta,
    val appliedEquippedItems: EquipmentManager.AppliedEquippedItems,
) : Event(!Bukkit.isPrimaryThread()) {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
