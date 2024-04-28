package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.plus
import net.craftventure.bukkit.ktx.extension.plusAssign
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.getSafeEyeMatrix
import net.craftventure.core.manager.EquipmentManager
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector

@JsonClass(generateAdapter = true)
class VelocityItemUseEffect(
    val random: Vector? = null,
    val static: Vector? = null,
    val directional: Vector? = null,
    val relative: Boolean = false,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val velocity = Vector()
        static?.let { velocity += it }
        random?.let {
            velocity.x += (it.x * 2 * CraftventureCore.getRandom().nextDouble()) - it.x
            velocity.y += (it.y * 2 * CraftventureCore.getRandom().nextDouble()) - it.y
            velocity.z += (it.z * 2 * CraftventureCore.getRandom().nextDouble()) - it.z
        }
        directional?.let {
            val matrix = player.getSafeEyeMatrix()
            if (matrix != null) {
                val directionalUpdated = it.clone()
                matrix.transformPoint(directionalUpdated)
                velocity += directionalUpdated
            }
        }
        if (relative)
            player.velocity = player.velocity + velocity
        else
            player.velocity = velocity
    }
}