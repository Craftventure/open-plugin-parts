package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.manager.EquipmentManager
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class SendChatItemUseEffect(
    val message: String,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val parsed = message.parseWithCvMessage()
        player.sendMessage(parsed)
    }
}