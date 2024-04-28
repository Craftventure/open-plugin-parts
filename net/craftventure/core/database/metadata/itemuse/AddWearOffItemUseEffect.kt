package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.ConsumptionEffectTracker
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class AddWearOffItemUseEffect(
    val category: String,
    val duration: DurationJson,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val meta = player.getMetadata<ConsumptionEffectTracker>() ?: return
        meta.addWearOff(ConsumptionEffectTracker.WearOff.ofDuration(category, duration.duration))
    }
}