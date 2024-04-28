package net.craftventure.core.database.metadata.itemwear

import com.squareup.moshi.JsonClass
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.EquippedItemsMeta
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class PeriodicItemWearEffect(
    val duration: DurationJson,
    val randomDuration: DurationJson? = null,
    val effects: List<ItemWearEffect>,
) : ItemWearEffect() {
    @Transient
    private var currentTick = 0

    @Transient
    private var currentPeriodTicks = duration.asTicksInt()

    override fun applyActual(
        player: Player,
        playerMatrix: Matrix4x4,
        headMatrix: Matrix4x4,
        data: EquipmentManager.EquippedItemData,
        meta: EquippedItemsMeta
    ) {
        currentTick++
        if (currentTick < currentPeriodTicks) {
            return
        }
        if (randomDuration != null)
            currentPeriodTicks =
                duration.asTicksInt() + CraftventureCore.getRandom().nextInt(randomDuration.asTicksInt())
        currentTick = 0

        effects.forEach { it.apply(player, playerMatrix, headMatrix, data, meta) }
    }
}