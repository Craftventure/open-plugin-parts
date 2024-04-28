package net.craftventure.core.effect

import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.dropNaturally
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.absoluteValue

class ItemEmitter(
    name: String,
    val location: Location,
    val itemStack: ItemStack,
    val randomOffset: Vector = Vector(0, 0, 0),
    val startVelocity: Vector = Vector(0, 0, 0),
    val randomVelocity: Vector = Vector(0, 0, 0),
    val spawnRate: Float = 1f,
    val lifeTimeTicksMin: Int = 20,
    val lifeTimeTicksMax: Int = 20
) : BaseEffect(name) {
    private var unspawnedTicks = 0

    override fun update(tick: Int) {
        if (spawnRate > 0) {
            for (i in 0..spawnRate.toInt()) {
                spawnItem()
            }
        } else {
            val ticksToWait = 1 / spawnRate.absoluteValue
            if (ticksToWait <= unspawnedTicks) {
                spawnItem()
                unspawnedTicks = 0
            } else {
                unspawnedTicks++
            }
        }
    }

    private fun spawnItem() {
        val itemStack = this.itemStack.clone()
        itemStack.durability = (Math.random() * Short.MAX_VALUE).toInt().toShort()
        val item = this.location.clone()
            .add(randomOffset(randomOffset.x), randomOffset(randomOffset.y), randomOffset(randomOffset.z))
            .dropNaturally(this.itemStack)
        item.pickupDelay = Int.MAX_VALUE
        item.velocity = this.startVelocity.clone().apply {
            x += randomOffset(randomVelocity.x)
            y += randomOffset(randomVelocity.y)
            z += randomOffset(randomVelocity.z)
        }
//        Logger.info("Velocity ${item.velocity}")

        val lifeTime = when {
            lifeTimeTicksMin == lifeTimeTicksMax -> lifeTimeTicksMin
            else -> (lifeTimeTicksMin..lifeTimeTicksMax).random()
        }
        executeSync(lifeTime.toLong()) { item.remove() }
    }

    private fun randomOffset(base: Double) = ((CraftventureCore.getRandom().nextFloat() * 2) - 1) * base
}