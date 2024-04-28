package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.manager.EquipmentManager
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class ChancedItemUseEffect(
    val chance: Double,
    val effects: List<ItemUseEffect>,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val random = CraftventureCore.getRandom().nextDouble()
        logcat { "Executing? ${random.format(2)} <= ${chance.format(2)} == ${random <= chance}" }
        if (random <= chance) {
            effects.forEach { it.apply(player, location, data) }
        }
    }

    override fun shouldBlockConsumption(player: Player): Component? =
        super.shouldBlockConsumption(player) ?: effects.firstNotNullOfOrNull { it.shouldBlockConsumption(player) }
}