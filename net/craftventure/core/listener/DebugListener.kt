package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.asString
import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.manager.PlayerStateManager
import okhttp3.*
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.spigotmc.event.entity.EntityDismountEvent
import java.io.IOException

class DebugListener : Listener {
//    @EventHandler(ignoreCancelled = true)
//    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
//        if (CraftventureCore.getEnvironment() == CraftventureCore.Environment.DEVELOPMENT) {
//
//        }
//    }

//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
//    fun debugTeleport(event: PlayerTeleportEvent) {
//        logcat {
//            "Player ${event.player.name} teleported? ${event.cause} ${event.isCancelled} to=${event.to.x.format(2)}, ${
//                event.to.y.format(
//                    2
//                )
//            }, ${event.to.z.format(2)}"
//        }
////        logcat { Logger.miniTrace(15) }
////        if (event.cause == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
////        Thread.dumpStack()
////        }
//    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
//
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
//
//    }

//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    fun onChunkLoad(event: ChunkLoadEvent) {
//        if (event.chunk.entities.isNotEmpty())
//            "A chunk was loaded with ${event.chunk.entities.size} entities in it".broadcastAsDebugTimings()
//    }

    //    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
//    fun onPlayerMove(playerMoveEvent: PlayerMoveEvent) {
//        Logger.console("PlayerMoveEvent %s", playerMoveEvent.isCancelled)
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
//    fun onPlayerMove(playerActualMoveEvent: PlayerActualMoveEvent) {
//        Logger.console("PlayerActualMoveEvent %s", playerActualMoveEvent.isCancelled)
//    }
//
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    fun onChunkUnload(event: ChunkUnloadEvent) {
//        if (CraftventureCore.getSettings().border.overlaps(event.chunk)) {
//            event.isCancelled = true
//        }
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerAnimationEvent(event: PlayerAnimationEvent) {
//        Logger.console("PlayerAnimationEvent " + event.animationType.name)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
//        Logger.console("PlayerInteractEntityEvent " + event.rightClicked)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerInteractEvent(event: PlayerInteractEvent) {
//        Logger.console("PlayerInteractEvent " + event.action.name)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerItemDamageEvent(event: PlayerItemDamageEvent) {
//        Logger.console("PlayerItemDamageEvent " + event.item)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerItemConsumeEvent(event: PlayerItemConsumeEvent) {
//        Logger.console("PlayerItemConsumeEvent " + event.item)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerPickupItemEvent(event: PlayerPickupItemEvent) {
//        Logger.console("PlayerPickupItemEvent " + event.item)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerRespawnEvent(event: PlayerRespawnEvent) {
//        Logger.console("PlayerRespawnEvent " + event.respawnLocation)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerSpawnLocationEvent(event: PlayerSpawnLocationEvent) {
//        Logger.console("PlayerSpawnLocationEvent " + event.spawnLocation)
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
//    fun PlayerToggleFlightEvent(event: PlayerToggleFlightEvent) {
//        Logger.console("PlayerToggleFlightEvent %s %s", event.isFlying, event.isCancelled)
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
//    fun PlayerToggleSneakEvent(event: PlayerToggleSneakEvent) {
//        Logger.console("PlayerToggleSneakEvent %s %s", event.isSneaking, event.isCancelled)
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
//    fun PlayerToggleSprintEvent(event: PlayerToggleSprintEvent) {
//        Logger.console("PlayerToggleSprintEvent %s %s", event.isSprinting, event.isCancelled)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerVelocityEvent(event: PlayerVelocityEvent) {
//        Logger.console("PlayerVelocityEvent " + event.velocity)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerItemBreakEvent(event: PlayerItemBreakEvent) {
//        Logger.console("PlayerItemBreakEvent " + event.brokenItem)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerGameModeChangeEvent(event: PlayerGameModeChangeEvent) {
//        Logger.console("PlayerGameModeChangeEvent " + event.newGameMode.name)
//    }
//
//    @EventHandler(ignoreCancelled = false)
//    fun PlayerFishEvent(event: PlayerFishEvent) {
//        Logger.console("PlayerFishEvent " + event.player.name)
//    }
//
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onEntityDismountEvent(event: EntityDismountEvent) {
        if (true) return
        val dismounted = event.dismounted
        val dismountedLocation = dismounted.location
        val player = event.entity as? Player ?: return run {
            logcat { "No player" }
        }
        val meta = player.getMetadata<PlayerStateManager.PlayerState>() ?: return run {
            logcat { "No meta" }
        }
        val ride = meta.ride ?: return run {
            logcat { "No ride" }
        }

        executeSync(5) {
            if (meta.ride != null && !player.isInsideVehicle && player.gameMode != GameMode.SPECTATOR) {
                try {
                    CvApi.okhttpClient.newCall(
                        Request.Builder()
                            .url(CraftventureCore.getSettings().errorWebhook)
                            .post(
                                MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart(
                                        "content",
                                        "`Ride ${ride.displayName()} bugging for ${player.name} at ${
                                            dismountedLocation.toVector().asString()
                                        } valid=${dismounted.isValid} playerLoc=${
                                            player.location.toVector().asString()
                                        }`"
                                    )
                                    .build()
                            )
                            .build()
                    ).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            logcat { "Responded" }
                            response.close()
                        }

                    })
                } catch (e: Exception) {
                }
            }
        }
    }
}
