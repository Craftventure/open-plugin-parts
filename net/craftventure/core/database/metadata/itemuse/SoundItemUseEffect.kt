package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.manager.EquipmentManager
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class SoundItemUseEffect(
    val sounds: List<String>,
    val volume: Float = 1f,
    val pitch: Float = 1f,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val sound = sounds.randomOrNull() ?: return
        player.world.playSound(player.location, sound, volume, pitch)
    }
}