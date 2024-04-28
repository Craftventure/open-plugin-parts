package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.ConsumptionEffectTracker
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.math.roundToLong

@JsonClass(generateAdapter = true)
class DelayedItemUseEffect(
    val delay: DurationJson,
    val random: DurationJson? = null,
    val effects: List<ItemUseEffect>,
    val allowCancel: Boolean = true,
    val period: DurationJson? = null,
    val periods: Int? = null,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val ticks =
            delay.asTicks() + (random?.asTicks() ?: 0L).times(CraftventureCore.getRandom().nextDouble()).roundToLong()
//        logcat { "Executing $ticks for $delay" }
        val meta = player.getMetadata<ConsumptionEffectTracker>()

        if (periods != null && period != null) {
            val periodTime = period.asTicks()
            for (i in 0 until periods) {
                val at = ticks + (periodTime * i)

                meta?.register(at, allowCancellation = allowCancel) {
                    effects.forEach { it.apply(player, location, data) }
                }
            }
        } else {
            meta?.register(ticks, allowCancellation = allowCancel) {
                effects.forEach { it.apply(player, location, data) }
            }
        }
    }

    override fun shouldBlockConsumption(player: Player): Component? =
        super.shouldBlockConsumption(player) ?: effects.firstNotNullOfOrNull { it.shouldBlockConsumption(player) }
}