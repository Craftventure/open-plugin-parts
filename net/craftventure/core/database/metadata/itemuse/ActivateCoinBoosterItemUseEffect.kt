package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.task.ActiveMoneyRewardTask
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class ActivateCoinBoosterItemUseEffect(
    val coinBoosterId: String,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val coinBooster = MainRepositoryProvider.coinBoosterRepository.findCached(coinBoosterId)
        if (coinBooster == null) {
            logcat(
                LogPriority.ERROR,
                logToCrew = true
            ) { "Failed to find coinbooster $coinBoosterId for item ${data.id}" }
            return
        }

        if (MainRepositoryProvider.activeCoinBoosterRepository.activate(player.uniqueId, coinBooster)) {
            player.sendMessage(Translation.MENU_COINBOOSTER_ACTIVATED.getTranslation(player)!!)
        } else {
            player.sendMessage(Translation.MENU_COINBOOSTER_ACTIVATION_FAILED.getTranslation(player)!!)
            logcat(LogPriority.ERROR) { "Failed to find activate $coinBoosterId for item ${data.id}" }
            return
        }
    }

    override fun shouldBlockConsumption(player: Player) =
        if (ActiveMoneyRewardTask.getActivePersonalBoosters(player).isNotEmpty())
            Component.text("This item would activate more personal coinboosters than possible")
        else null
}