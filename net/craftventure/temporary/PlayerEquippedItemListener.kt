package net.craftventure.temporary

import net.craftventure.bukkit.ktx.extension.player
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerEquippedItem
import net.craftventure.database.repository.BaseIdRepository

class PlayerEquippedItemListener : BaseIdRepository.Listener<PlayerEquippedItem>() {
    override fun onMerge(item: PlayerEquippedItem) {
        handle(item)
    }

    override fun onInsert(item: PlayerEquippedItem) {
        handle(item)
    }

    override fun onUpdate(item: PlayerEquippedItem) {
        handle(item)
    }

    override fun onDelete(item: PlayerEquippedItem) {
        handle(item)
    }

    override fun onRefresh(item: PlayerEquippedItem) {
        handle(item)
    }

    private fun handle(item: PlayerEquippedItem) {
        val player = item.uuid?.player ?: return
        Logger.debug("Changes for ${item.uuid}/${item.slot}")
        EquipmentManager.invalidatePlayerEquippedItems(player)
    }
}