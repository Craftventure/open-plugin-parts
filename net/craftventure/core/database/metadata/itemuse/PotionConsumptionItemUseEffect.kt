package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.renewPotionEffect
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

@JsonClass(generateAdapter = true)
class PotionConsumptionItemUseEffect(
    val potionEffectType: PotionEffectType,
    val duration: DurationJson,
    val amplifier: Int = 0,
    val ambient: Boolean = true,
    val particles: Boolean = false,
    val icon: Boolean = false,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        player.renewPotionEffect(
            potionEffectType = potionEffectType,
            duration = duration.asTicksInt(),
            amplifier = amplifier,
            ambient = ambient,
            particles = particles,
            icon = icon,
        )
    }
}