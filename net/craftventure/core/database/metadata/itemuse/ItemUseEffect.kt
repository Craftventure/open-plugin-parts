package net.craftventure.core.database.metadata.itemuse

import net.craftventure.core.manager.EquipmentManager
import net.craftventure.jsontools.PolymorphicHint
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player

@PolymorphicHint(
    types = [
//        PolymorphicHint.PolymorphicHintType(
//            ""
//        )
    ]
)
sealed class ItemUseEffect {
    abstract fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData)

    /**
     * Returns the reason if it's blocked
     */
    open fun shouldBlockConsumption(player: Player): Component? = null
}
