package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.ProjectileEvents.removeUponEnteringBubbleColumn
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.inventory.ItemStack

@JsonClass(generateAdapter = true)
class ProjectileItemUseEffect(
    val velocityMultiplier: Double,
    val customModelData: Int,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        player.launchProjectile(Snowball::class.java).apply {
            removeUponEnteringBubbleColumn()
            item = ItemStack(Material.SNOWBALL).apply {
                itemMeta = itemMeta?.apply {
                    setCustomModelData(this@ProjectileItemUseEffect.customModelData)
                }
            }
            velocity = velocity.normalize().multiply(this@ProjectileItemUseEffect.velocityMultiplier)
        }
    }
}