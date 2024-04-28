package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.util.TimeUtils.secondsFromNow
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.metadata.GenericPlayerMeta
import net.craftventure.core.serverevent.PacketPlayerSteerEvent
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffectType
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent


class MountListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        if (event.player.gameMode == GameMode.CREATIVE) return
        val wornData = event.player.equippedItemsMeta()?.appliedEquippedItems
        if (wornData?.shoulderPetLeft != null || wornData?.shoulderPetRight != null)
            EquipmentManager.reapply(event.player)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPotionEffect(event: EntityPotionEffectEvent) {
        val player = event.entity as? Player ?: return
        if (event.newEffect?.type == PotionEffectType.INVISIBILITY || event.oldEffect?.type == PotionEffectType.INVISIBILITY) {
            EquipmentManager.reapply(player)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        EquipmentManager.reapply(event.player)
//        Logger.info("Gamemode of ${event.player.name} changed to ${event.newGameMode}")
//        if (event.newGameMode == GameMode.ADVENTURE)
//            event.player.resetFloatingState()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onEntityMount(event: EntityMountEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
//            player.resetFloatingState()
//            Logger.info("${player.name} mounted")
            executeSync {
                EquipmentManager.reapply(player)
            }
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onEntityDismount(event: EntityDismountEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
//            player.resetFloatingState()
            //            Logger.console(player.getName() + " dismounted");
            executeSync {
                EquipmentManager.reapply(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        val meta = player.getMetadata<GenericPlayerMeta>() ?: return
        val blocked = meta.isSneakingBlocked

//        logcat { "ToggleSneak blocked=$blocked sneak=${event.isSneaking}" }

        if (blocked && event.isSneaking) {
            event.isCancelled = true
//            logcat { "ToggleSneak Blocked" }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPacketPlayerSteer(event: PacketPlayerSteerEvent) {
        val player = event.player
        val meta = player.getMetadata<GenericPlayerMeta>() ?: return
        val blocked = meta.isManualVehicleExitingBlocked
        val inVehicle = player.isInsideVehicle

//        logcat { "PlayerSteer blocked=$blocked inVehicle=$inVehicle isDismounting=${event.isDismounting}" }

        if (blocked && inVehicle && event.isDismounting) {
            event.isCancelled = true
            MessageBarManager.display(
                player,
                MessageBarManager.Message(
                    id = "vehicle_exit_blocked",
                    text = Component.text("Exiting this vehicle is temporarily blocked", CVTextColor.serverNotice),
                    type = MessageBarManager.Type.VEHICLE_EXIT_BLOCKED,
                    untilMillis = secondsFromNow(2.0),
                ),
                replace = true,
            )
//            logcat { "PlayerSteer Blocked" }
        }
    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onVehicleEnter(event: VehicleEnterEvent) {
//        if (event.entered is Player) {
//            val player = event.entered as Player
//            //            Logger.console(player.getName() + " entered vehicle? " + event.isCancelled());
//        }
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onVehicleExit(event: VehicleExitEvent) {
//        if (event.exited is Player) {
//            val player = event.exited as Player
//            //            Logger.console(player.getName() + " exited vehicle? " + event.isCancelled());
//        }
//    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    fun PlayerToggleSneakEvent(event: PlayerToggleSneakEvent) {
    //        Logger.console(event.getPlayer().getName() + " sneaking? " + event.isCancelled());
//    }

    //    @EventHandler(priority = EventPriority.MONITOR)
    //    public void onEntityDismountEvent(PacketPlayerSteerEvent event) {
    //        Logger.console(event.getPlayer().getName() + " steered vehicle? " + event.isCancelled());
    //    }
}
