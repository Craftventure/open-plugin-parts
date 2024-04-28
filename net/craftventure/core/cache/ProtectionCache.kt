package net.craftventure.core.cache

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.toLocation
import net.craftventure.database.generated.cvdata.tables.pojos.Protection
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.type.Door
import org.bukkit.block.data.type.Gate
import org.bukkit.block.data.type.Switch
import org.bukkit.command.CommandSender
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ProtectionCache {
    private var protections: Map<String, Protection>? = null

    fun cleanup(initiator: CommandSender? = null, dryRun: Boolean = false) {
        executeAsync {
            val database = MainRepositoryProvider.protectionRepository
            val protections = database.itemsPojo()
            executeSync {
                var edited = 0
                var deleted = 0

                var toDelete = mutableListOf<Protection>()
                var toAdd = mutableListOf<Protection>()

                for (protection in protections) {
                    val block = protection.toLocation().block
                    val targetBlock = getTargetBlock(block)

                    if (!isProtectable(targetBlock.type)) {
                        initiator?.sendMessage(CVTextColor.serverNotice + "Deleting block at x=${block.x} y=${block.y} z=${block.z} of type ${block.type} (unprotectable)")
                        toDelete.add(protection)
                        deleted++
                    } else if (block != targetBlock) {
                        initiator?.sendMessage(CVTextColor.serverNotice + "Replacing block at x=${block.x} y=${block.y} z=${block.z} of type ${block.type} (not the target block)")
                        toDelete.add(protection)
                        toAdd.add(
                            Protection(
                                UUID.randomUUID(),
                                targetBlock.location.world.name,
                                targetBlock.location.blockX,
                                targetBlock.location.blockY,
                                targetBlock.location.blockZ,
                                protection.autoClose,
                                protection.permission,
                                protection.server
                            )
                        )
                        edited++
                    }
                }

                executeAsync {
                    try {
                        for (delete in toDelete)
                            database.delete(delete)
                        for (add in toAdd)
                            database.create(add)
                        initiator?.sendMessage(CVTextColor.serverNotice + "Protection cleanup completed, $edited edited and $deleted deleted")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        initiator?.sendMessage(CVTextColor.serverNotice + "Protection cleanup failed to edit $edited and delete $deleted: ${e.message}")
                    }

                    updateCaches()
                }
            }
        }
    }

    fun updateCaches() {
        val protections = ConcurrentHashMap<String, Protection>()
        MainRepositoryProvider.protectionRepository.invalidateCaches()
        MainRepositoryProvider.protectionRepository.requireCache()
        MainRepositoryProvider.protectionRepository.itemsPojo().forEach { protection ->
            protections[cacheKey(protection)] = protection
        }
        this.protections = protections
    }

    fun cacheKey(protection: Protection) =
        "${protection.world}:${protection.x}:${protection.y}:${protection.z}"

    fun cacheKey(location: Location) =
        "${location.world!!.name}:${location.blockX}:${location.blockY}:${location.blockZ}"

    fun get(cacheKey: String) = protections?.get(cacheKey)
    fun get(location: Location) = get(cacheKey(location))
    fun get(block: Block) = get(cacheKey(block.location))

    fun getTargetBlock(block: Block): Block {
        return block.let {
            val data = it.blockData
//            Logger.info("Data of type $data")
            when (data) {
                is Door -> {
//                    Logger.info("Door ${data.hinge} ${block.type}")
                    if (data.half == Bisected.Half.TOP)
                        return@let it.getRelative(BlockFace.DOWN)
                }
            }
            return@let it
        }
    }

    fun isProtectable(material: Material): Boolean = when {
        material.data.isAssignableFrom(Door::class.java) -> true
        material.data.isAssignableFrom(Switch::class.java) -> true
        material.data.isAssignableFrom(Gate::class.java) -> true
//        material.data.isAssignableFrom(Powerable::class.java) -> true
//        material.data.isAssignableFrom(Openable::class.java) -> true
        else -> false
    }
}
