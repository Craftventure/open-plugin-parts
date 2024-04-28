package net.craftventure.core.extension

import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

fun <T : Entity> Location.spawn(
    entityType: Class<T>,
    consumer: ((Entity) -> Unit)? = null,
    persistent: Boolean = false
): T =
    world!!
        .spawn(this, entityType, consumer).apply {
            isPersistent = persistent
        }

inline fun <reified T : Entity> Location.spawn(
    noinline consumer: ((Entity) -> Unit)? = null,
    persistent: Boolean = false
): T =
    world!!
        .spawn(this, T::class.java) { entity ->
            consumer?.invoke(entity)
            if (entity.vehicle != null) {
//        Logger.debug("Removed vehicle for ${entity.type.name}")
                entity.vehicle!!.remove()
            }
        }
        .apply {
            isPersistent = persistent
        }

fun Location.spawnFallingBlock(data: BlockData) = this.world.spawnFallingBlock(this, data)

fun Location.drop(itemStack: ItemStack): Item = world!!.dropItem(this, itemStack)
fun Location.dropNaturally(itemStack: ItemStack): Item = world!!.dropItemNaturally(this, itemStack)
