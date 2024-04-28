package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.actions.AtAtAction
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class AtAtBlasterItemUseEffect(
    val speed: Double = CoasterMathUtils.kmhToBpt(50.0),
    val maxDistance: Double = 15.0,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        AtAtAction.createVisualProjectile(
            player.eyeLocation,
            player.location.direction,
            speed = speed,
            maxDistance = maxDistance
        )
    }
}