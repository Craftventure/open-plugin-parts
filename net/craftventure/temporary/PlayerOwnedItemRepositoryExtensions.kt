package net.craftventure.temporary

import net.craftventure.bukkit.ktx.extension.player
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.database.bukkit.extensions.getItemName
import net.craftventure.database.repository.PlayerOwnedItemRepository
import java.util.*


fun PlayerOwnedItemRepository.create(
    uuid: UUID,
    ownableItemId: String,
    paidPrice: Int,
    times: Int = 1,
    showReceiveMessage: Boolean = true,
    updateWornItems: Boolean = true,
    disableListeners: Boolean = false,
): Boolean {
    return create(uuid, ownableItemId, paidPrice, times, disableListeners = disableListeners).also {
        if (updateWornItems)
            uuid.player?.let {
                EquipmentManager.invalidatePlayerEquippedItems(it)
            }
        if (showReceiveMessage && it) {
            executeAsync {
                uuid.player?.sendMessage(
                    CVTextColor.serverNotice + "You received a new item: ${
                        getItemName(
                            ownableItemId
                        )
                    }"
                )
            }
        }
    }
}


fun PlayerOwnedItemRepository.createOneLimited(
    uuid: UUID,
    ownableItemId: String,
    paidPrice: Int,
    showReceiveMessage: Boolean = true,
): Boolean = createOneLimited(uuid, ownableItemId, paidPrice).also {
    if (showReceiveMessage && it) {
        executeAsync {
            uuid.player?.sendMessage(
                CVTextColor.serverNotice + "You received a new item: ${
                    getItemName(
                        ownableItemId
                    )
                }"
            )
        }
    }
}