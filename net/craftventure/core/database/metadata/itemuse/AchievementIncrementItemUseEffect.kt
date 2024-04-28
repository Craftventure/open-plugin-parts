package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class AchievementIncrementItemUseEffect(
    val achievementId: String,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        MainRepositoryProvider.achievementProgressRepository.increaseCounter(player.uniqueId, achievementId)
    }
}