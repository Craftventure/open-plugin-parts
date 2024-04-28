package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.spawnParticlesAround
import net.craftventure.bukkit.ktx.extension.toggleOpenable
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.cache.ProtectionCache
import net.craftventure.core.extension.getMeta
import net.craftventure.core.utils.Tools
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.Protection
import org.bukkit.Color
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.block.data.type.CoralWallFan
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*


class ProtectionListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        val blockType = event.block.type
        if (blockType.data.isAssignableFrom(TrapDoor::class.java)) {
            event.isCancelled = true
        }/* else {
            val isLadder = event.changedType == Material.LADDER || blockType == Material.LADDER
            if (isLadder) {
//                Logger.debug("changedType=${event.changedType} is=${event.block.type}")
                event.isCancelled = true
            }
        }*/
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onHangingBreak(event: HangingBreakEvent) {
        if (event.cause == HangingBreakEvent.RemoveCause.PHYSICS)
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockFade(event: BlockFadeEvent) {
        val blockType = event.block.type
        when {
            blockType == Material.SNOW || blockType == Material.SNOW_BLOCK -> event.isCancelled = true
            blockType == Material.ICE || blockType == Material.BLUE_ICE || blockType == Material.FROSTED_ICE || blockType == Material.PACKED_ICE -> event.isCancelled =
                true

            blockType.data.isAssignableFrom(CoralWallFan::class.java) -> event.isCancelled = true
            blockType.name.contains("CORAL") -> event.isCancelled = true
        }
//        Logger.debug("${event.block.type} fading to ${event.newState.type} cancelled=${event.isCancelled}")
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onProtectedInteraction(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        val clickedBlock = event.clickedBlock ?: return
        val protectionTarget = ProtectionCache.getTargetBlock(clickedBlock)
        val player = event.player

        val isUsingProtectorTool =
            player.inventory.itemInMainHand.getMeta(Tools.KEY_TOOL_ID) == Tools.PROTECTION_TOOL_ID && player.isCrew()

//            Logger.info("Finding ${ProtectionCache.cacheKey(clickedBlock.location)}")
        val cachedProtection = ProtectionCache.get(protectionTarget)
        val shouldBlock = !player.isCrew()
        val shouldNotice = player.isCrew()
        val type = protectionTarget.type
        val isProtectable = ProtectionCache.isProtectable(type)

//        Logger.info("Interacted block: material=$type shouldBlock=$shouldBlock shouldNotice=$shouldNotice isProtectable=$isProtectable protected=${cachedProtection != null}")

        if (cachedProtection != null) {
            if (isUsingProtectorTool) {
                event.isCancelled = true
                event.clickedBlock?.state?.update(true, true)

                if (event.action == Action.LEFT_CLICK_BLOCK) {
                    executeAsync {
                        try {
                            val deleted = MainRepositoryProvider.protectionRepository.delete(cachedProtection)
                            player.sendMessage(CVTextColor.serverNotice + if (deleted) "Protection deleted" else "Protection was not deleted")
                            if (deleted) {
                                ProtectionCache.updateCaches()
                                protectionTarget.spawnParticlesAround(
                                    Particle.REDSTONE,
                                    data = DustOptions(Color.RED, 2f)
                                )
                            } else {
                                protectionTarget.spawnParticlesAround(
                                    Particle.REDSTONE,
                                    data = DustOptions(Color.GREEN, 2f)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    player.sendMessage(CVTextColor.serverNotice + "This block is public")
                    protectionTarget.spawnParticlesAround(Particle.REDSTONE, 0, -1.0, 1.0, 0.0, 1.0)
                }
            } else {
                if (shouldNotice)
                    player.sendMessage((CVTextColor.serverNotice + "That ") + (CVTextColor.serverNoticeAccent + type.name) + (CVTextColor.serverNotice + " block is public"))

                val permission = cachedProtection.permission
                if (permission == null || player.hasPermission(permission)) {
                    openIfIronDoor(protectionTarget)
                } else {
                    event.isCancelled = true
                }
            }
        } else if (isProtectable) {
            if (isUsingProtectorTool) {
                event.isCancelled = true
                if (event.action == Action.LEFT_CLICK_BLOCK) {
                    player.sendMessage(CVTextColor.serverNotice + "Creating protection...")
                    val protection = Protection(
                        UUID.randomUUID(),
                        protectionTarget.location.world.name,
                        protectionTarget.location.blockX,
                        protectionTarget.location.blockY,
                        protectionTarget.location.blockZ,
                        false,
                        null,
                        "craftventure"
                    )
                    executeAsync {
                        try {
                            val created = MainRepositoryProvider.protectionRepository.create(protection)
                            player.sendMessage(CVTextColor.serverNotice + if (created) "Protection created" else "Protection was not created")
                            ProtectionCache.updateCaches()
                            executeSync {
                                protectionTarget.spawnParticlesAround(
                                    Particle.REDSTONE,
                                    data = DustOptions(if (created) Color.GREEN else Color.RED, 2f)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            player.sendMessage(CVTextColor.serverError + "Failed to create protection: ${e.message} (see console for more info)")
                            executeSync {

                                protectionTarget.spawnParticlesAround(
                                    Particle.REDSTONE,
                                    data = DustOptions(Color.RED, 2f)
                                )
                            }
                        }
                    }
                } else {
                    player.sendMessage(CVTextColor.serverNotice + "This block is not public")
                    protectionTarget.spawnParticlesAround(Particle.REDSTONE)
                }
            } else {
                if (player.isCrew()) {
                    openIfIronDoor(protectionTarget)
                }
            }

            if (shouldBlock)
                event.isCancelled = true
        }
    }

    private fun openIfIronDoor(protectionTarget: Block) {
        if (protectionTarget.type == Material.IRON_DOOR) {
            protectionTarget.toggleOpenable()
            protectionTarget.world.playEffect(protectionTarget.location, Effect.IRON_DOOR_TOGGLE, 0)
        }
    }
}
