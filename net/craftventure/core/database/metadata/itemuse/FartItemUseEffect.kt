package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.listener.AprilFoolsListener
import net.craftventure.core.manager.EquipmentManager
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class FartItemUseEffect() : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        AprilFoolsListener.fart(player)
    }
}