package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.utils.FireworkUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.time.DurationUnit

@JsonClass(generateAdapter = true)
class FireworkItemUseEffect(
    val velocityX: Double = 0.0,
    val velocityY: Double = 0.0,
    val velocityZ: Double = 0.0,
    val lifetime: DurationJson? = null,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val item =
            MainRepositoryProvider.itemStackDataRepository.cachedItems.firstOrNull { it.id == data.id }
//                    Logger.debug("Found item = ${item != null}")
        if (item != null) {
            item.itemStack?.let { itemStack ->
                FireworkUtils.spawn(
                    location = location,
                    itemStack = itemStack,
                    shooter = player,
                    velocityX = velocityX,
                    velocityY = velocityY,
                    velocityZ = velocityZ,
                    lifeTimeInSeconds = lifetime?.toDouble(DurationUnit.SECONDS),
                )
            }
        }
    }
}