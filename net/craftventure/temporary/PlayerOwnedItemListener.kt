package net.craftventure.temporary

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.player
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.OwnedItemCache
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.database.repository.BaseIdRepository
import org.bukkit.entity.Player

class PlayerOwnedItemListener : BaseIdRepository.Listener<PlayerOwnedItem>() {
    override fun onMerge(item: PlayerOwnedItem) {
        item.uuid!!.player?.let { update(it) }
    }

    override fun onInsert(item: PlayerOwnedItem) {
        item.uuid!!.player?.let { update(it) }
    }

    override fun onUpdate(item: PlayerOwnedItem) {
        item.uuid!!.player?.let { update(it) }
    }

    override fun onDelete(item: PlayerOwnedItem) {
        item.uuid!!.player?.let { update(it) }
    }

    private fun update(player: Player) {
        player.getMetadata<OwnedItemCache>()?.update {
            EquipmentManager.invalidatePlayerEquippedItems(player)
        }
    }
}