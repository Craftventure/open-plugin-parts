package net.craftventure.bukkit.ktx.extension

import net.craftventure.core.ktx.util.Logger
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock
import org.spigotmc.SpigotConfig
import kotlin.math.max
import kotlin.math.min


fun World.getBoundingBoxForBlock(x: Int, y: Int, z: Int): AABB? {
    val pos = BlockPos(x, y, z)
    val world = (this as CraftWorld).handle
    try {
        return world.getBlockStateIfLoaded(pos)?.getShape(world, pos)?.takeIf { !it.isEmpty }?.bounds()
    } catch (e: Exception) {
        return null
    }
//    return world.getType(pos).g(null, pos)?.takeIf { !it.isEmpty }?.a() // << Actual shape
}

fun Block.getBoundingBoxes() = this.world.getBoundingBoxesForBlock(x, y, z)

fun World.getBoundingBoxesForBlock(x: Int, y: Int, z: Int): List<AABB>? {
    val pos = BlockPos(x, y, z)
    val world = (this as CraftWorld).handle
    try {
        return world.getBlockStateIfLoaded(pos)?.getCollisionShape(world, pos)?.takeIf { !it.isEmpty }?.toAabbs()
    } catch (e: Exception) {
        return null
    }
//    return world.getType(pos).g(null, pos)?.takeIf { !it.isEmpty }?.a() // << Actual shape
}

fun World.getItemDespawnRate() = (this as? CraftWorld)?.handle?.spigotConfig?.itemDespawnRate
    ?: SpigotConfig.config.getInt("world-settings.default.item-despawn-rate")

fun World.getHighestBlock(
    x: Int,
    z: Int,
    minY: Int = 0,
    maxY: Int = maxHeight,
    predicate: (Block) -> Boolean = { block -> block.type != Material.AIR }
): Block? {
    val highest = getHighestBlockYAt(x, z)
    for (y in (max(0, minY)..min(highest, maxY)).reversed()) {
        val block = getBlockAt(x, y, z)
        if (predicate(block)) {
            return block
        }
    }
    return null
}

fun World.getFrictionFactor(x: Int, y: Int, z: Int): Float? = this.getBlockAt(x, y, z).getFrictionFactor()

fun Block.getFrictionFactor(): Float? {
    try {
        val block = this as CraftBlock
        return block.nms.block.friction
    } catch (e: Throwable) {
        Logger.capture(e)
        return 0f
    }
}


fun Block.getMaterialColorRgb(): Int? {
    try {
        val block = this as CraftBlock
        val nmsBlockData: BlockState = block.nms
        val nmsBlock: net.minecraft.world.level.block.Block = nmsBlockData.block
        return nmsBlock.defaultMapColor().col
    } catch (e: Throwable) {
        Logger.capture(e)
        return null
    }
}

fun Block.getMaterialColorForMap(): Int? {
    try {
        val block = this as CraftBlock
        val nmsBlockData: BlockState = block.nms
        val nmsBlock: net.minecraft.world.level.block.Block = nmsBlockData.block
        return nmsBlock.defaultMapColor().id
    } catch (e: Throwable) {
        Logger.capture(e)
        return null
    }
}