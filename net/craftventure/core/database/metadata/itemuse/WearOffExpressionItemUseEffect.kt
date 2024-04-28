package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.core.ktx.util.sharedEvaluator
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.ConsumptionEffectTracker
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class WearOffExpressionItemUseEffect(
    val category: String,
    val booleanExpression: String,
    val effectsTrue: List<ItemUseEffect>,
    val effectsFalse: List<ItemUseEffect> = emptyList(),
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val meta = player.getMetadata<ConsumptionEffectTracker>() ?: return
        val wearOffDatas = meta.getWearOffs(category)
        if (sharedEvaluator.evaluateBoolean(
                booleanExpression, mapOf(
                    "size" to wearOffDatas.count()
                )
            )
        ) {
            effectsTrue.forEach { it.apply(player, location, data) }
        } else {
            effectsFalse.forEach { it.apply(player, location, data) }
        }
    }

    override fun shouldBlockConsumption(player: Player): Component? =
        super.shouldBlockConsumption(player) ?: effectsTrue.firstNotNullOfOrNull { it.shouldBlockConsumption(player) }
        ?: effectsFalse.firstNotNullOfOrNull { it.shouldBlockConsumption(player) }
}