package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.ride.PukeEffect
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsonClass(generateAdapter = true)
class PukeItemUseEffect(
    val offset: DurationJson = DurationJson(1.toDuration(DurationUnit.SECONDS)),
    val rewardAchievement: Boolean = true,
    val duration: DurationJson = DurationJson(3.toDuration(DurationUnit.SECONDS)),
    val isInstant: Boolean = false,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        PukeEffect.play(
            player,
            offset = offset.asTicksInt(),
            rewardAchievement = rewardAchievement,
            duration = duration.asTicksInt(),
            isInstant = isInstant
        )
    }
}