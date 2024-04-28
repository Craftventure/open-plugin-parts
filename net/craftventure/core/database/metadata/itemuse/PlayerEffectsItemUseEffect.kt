package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import org.bukkit.EntityEffect
import org.bukkit.Location
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
class PlayerEffectsItemUseEffect(
    val fireDuration: DurationJson? = null,
    val freezeDuration: DurationJson? = null,
    val hurt: Boolean? = null,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        if (fireDuration != null) {
            player.fireTicks = fireDuration.asTicksInt()
        }
        if (freezeDuration != null) {
            player.freezeTicks = freezeDuration.asTicksInt()
        }
        if (hurt == true) {
            player.playEffect(EntityEffect.HURT)
        }
    }
}