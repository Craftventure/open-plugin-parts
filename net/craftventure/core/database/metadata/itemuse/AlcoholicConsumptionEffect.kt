package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.renewPotionEffect
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsonClass(generateAdapter = true)
class AlcoholicConsumptionEffect(
    val duration: DurationJson = DurationJson(10.toDuration(DurationUnit.SECONDS)),
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        player.renewPotionEffect(
            potionEffectType = PotionEffectType.CONFUSION,
            duration = duration.asTicksInt(),
            amplifier = 0
        )
        executeAsync {
            MainRepositoryProvider.achievementProgressRepository
                .increaseCounter(player.uniqueId, "drink_alcohol")
        }
    }
}