package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.removeAllPotionEffects
import net.craftventure.core.manager.EquipmentManager
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

@JsonClass(generateAdapter = true)
class ClearPotionItemUseEffect(
    val clear: List<PotionEffectType> = emptyList(),
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        if (clear.isEmpty()) {
            player.removeAllPotionEffects()
        } else {
            clear.forEach {
                player.removePotionEffect(it)
            }
        }
    }
}