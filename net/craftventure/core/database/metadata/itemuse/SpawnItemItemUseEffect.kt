package net.craftventure.core.database.metadata.itemuse

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.getItemDespawnRate
import net.craftventure.bukkit.ktx.extension.plus
import net.craftventure.bukkit.ktx.extension.rotateY
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.drop
import net.craftventure.core.extension.setAge
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.utils.ItemStackUtils
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsonClass(generateAdapter = true)
class SpawnItemItemUseEffect(
    val item: String,
    val lifetime: DurationJson = DurationJson(5.toDuration(DurationUnit.SECONDS)),
    val velocityPower: Double = 1.0,
    val baseLocationOffset: Vector = Vector(),
    val randomOffset: Vector? = null,
    val forwardOffset: Vector = Vector(),
    val despawnEffects: List<ItemUseEffect> = emptyList(),
    val staticVelocity: Vector = Vector(),
    val randomVelocity: Vector? = null,
) : ItemUseEffect() {
    override fun apply(player: Player, location: Location, data: EquipmentManager.EquippedItemData) {
        val stack = ItemStackUtils.fromString(item) ?: return

        val baseLocation = player.location.clone()
        val location = baseLocation
            .add(baseLocationOffset)
            .add(forwardOffset.clone().apply {
                rotateY(Math.toRadians(-baseLocation.yaw.toDouble()))
            })
        randomOffset?.let {
            location.x += (it.x * 2 * CraftventureCore.getRandom().nextDouble()) - it.x
            location.y += (it.y * 2 * CraftventureCore.getRandom().nextDouble()) - it.y
            location.z += (it.z * 2 * CraftventureCore.getRandom().nextDouble()) - it.z
        }

        val velocity = location.direction.normalize().multiply(velocityPower) + staticVelocity
        randomVelocity?.let {
            velocity.x += (it.x * 2 * CraftventureCore.getRandom().nextDouble()) - it.x
            velocity.y += (it.y * 2 * CraftventureCore.getRandom().nextDouble()) - it.y
            velocity.z += (it.z * 2 * CraftventureCore.getRandom().nextDouble()) - it.z
        }
        val item = location.drop(stack)
        item.pickupDelay = Integer.MAX_VALUE
        item.setAge(location.world!!.getItemDespawnRate() - lifetime.asTicksInt())
        item.velocity = velocity

        executeSync(lifetime.asTicks()) {
            despawnEffects.forEach { it.apply(player, item.location, data) }
        }
    }

    override fun shouldBlockConsumption(player: Player): Component? =
        super.shouldBlockConsumption(player)
            ?: despawnEffects.firstNotNullOfOrNull { it.shouldBlockConsumption(player) }
}