package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class RewardItemItemUseEffect(
    val itemId: String,
    val announce: Boolean = true,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        executeAsync {
            if (MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(player.uniqueId, itemId, 0)) {
                if (!announce) return@executeAsync

                val item = MainRepositoryProvider.ownableItemRepository.findCached(itemId, loadIfCacheInvalid = false)
                val stack =
                    item?.guiItemStackDataId?.let { MainRepositoryProvider.itemStackDataRepository.findCached(it) }
                if (stack?.overridenTitle != null)
                    player.sendMessage(CVTextColor.serverNotice + "You've received the item ${stack.overridenTitle}!")
                else
                    player.sendMessage(CVTextColor.serverNotice + "You've received a new item!")
            }
        }
    }
}