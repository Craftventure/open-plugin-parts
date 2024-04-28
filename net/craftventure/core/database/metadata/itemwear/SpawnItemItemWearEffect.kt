package net.craftventure.core.database.metadata.itemwear

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.getItemDespawnRate
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.drop
import net.craftventure.core.extension.setAge
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.EquippedItemsMeta
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsonClass(generateAdapter = true)
class SpawnItemItemWearEffect(
    val item: String,
    val lifetime: DurationJson = DurationJson(5.toDuration(DurationUnit.SECONDS)),
    val offset: Vector = Vector(0.0, 0.0, 0.0),
    val velocity: Vector? = null,
    val despawnEffects: List<ItemWearEffect> = emptyList(),
    val useHeadMatrix: Boolean = false,
) : ItemWearEffect() {
    override fun applyActual(
        player: Player,
        playerMatrix: Matrix4x4,
        headMatrix: Matrix4x4,
        data: EquipmentManager.EquippedItemData,
        meta: EquippedItemsMeta
    ) {
        val stack = ItemStackUtils.fromString(item) ?: return

        val offset = this.offset.clone()
        val matrix = if (useHeadMatrix) headMatrix else playerMatrix
        matrix.transformPoint(offset)

        val location = offset.toLocation(player.world)

        val item = location.drop(stack)
        item.pickupDelay = Integer.MAX_VALUE
        item.setAge(location.world!!.getItemDespawnRate() - lifetime.asTicksInt())

        if (velocity != null) {
            val velocity = velocity.clone()
            matrix.transformPoint(velocity)
            velocity.subtract(matrix.toVector())
//            logcat { "Velocity ${velocity.asString()}" }
            item.velocity = velocity
        }

        executeSync(lifetime.asTicks()) {
            despawnEffects.forEach { it.apply(player, playerMatrix, headMatrix, data, meta) }
        }
    }
}