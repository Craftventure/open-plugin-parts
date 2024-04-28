package net.craftventure.core.ride.trackedride

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import net.craftventure.bukkit.ktx.entitymeta.EntityEvents.addListener
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.Deny
import net.craftventure.core.manager.PlayerStateManager.isAllowedToManuallyJoinRide
import net.craftventure.core.ride.trackedride.config.SceneItem
import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent

object TrackedRideHelper {
    fun checkSceneItemDuplicates(scenes: List<SceneItem>) {
        if (!PluginProvider.isTestServer()) return
        val groupedItems = scenes.groupBy { it.groupId + "/" + it.name + "/" + it.type }
        groupedItems.entries.forEach { entry ->
            if (entry.value.size > 1) {
                Logger.debug("${entry.key} matched ${entry.value.size} times: ${entry.value.joinToString(", ")}")
            }
        }
    }

    @JvmStatic
    fun getAllowedToManuallyJoinRide(player: Player) = player.isAllowedToManuallyJoinRide()

    @JvmStatic
    fun setCarEntity(entity: Entity, car: RideCar) {
        val listener = CarSeatListener(entity, car)
        entity.addListener(listener)
    }

    @JvmStatic
    fun setCarModelEntity(entity: Entity, car: RideCar) {
        val listener = CarModelListener(entity, car)
        entity.addListener(listener)
    }

    class CarSeatListener(val entity: Entity, val car: RideCar, val allowUnderwater: Boolean = true) : Listener {
        var debug = false

        @EventHandler
        fun onVehicleEnter(event: VehicleEnterEvent) {
            if (debug)
                Logger.debug("VehicleEnterEvent")
            val player = event.entered as? Player ?: return
            val enterState = car.attachedTrain.trackedRide.canEnter(player, true)

            if (enterState is Deny) {
                player.sendMessage(Component.text("Can't enter: ${enterState.reason}", CVTextColor.serverError))
                Logger.debug("Cancelling enter")
                event.isCancelled = true
                return
//                }
            } else {
                event.isCancelled = false
            }
        }

        @EventHandler
        fun onVehicleExit(event: VehicleExitEvent) {
            if (debug)
                Logger.debug("VehicleExitEvent")
            event.isCancelled = false
        }

        @EventHandler
        fun onEntityDamage(event: EntityDamageEvent) {
            if (debug)
                Logger.debug("EntityDamageEvent")
            event.isCancelled = true
        }

        @EventHandler
        fun onEntityDamage(event: EntityDamageByEntityEvent) {
            if (debug)
                Logger.debug("EntityDamageByEntityEvent")
            event.isCancelled = true
        }

        @EventHandler
        fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
            if (debug)
                Logger.debug("PlayerArmorStandManipulateEvent")
            event.isCancelled = true
        }

        @EventHandler
        fun onSpectatorTargetChanged(event: PlayerStartSpectatingEntityEvent) {
            if (debug)
                Logger.debug("PlayerStartSpectatingEntityEvent")
            car.trackedRide!!.onDismounted(car, event.player, event.currentSpectatorTarget)
        }

        @EventHandler
        fun onEntityMountEvent(event: EntityMountEvent) {
            if (debug)
                Logger.debug("EntityMountEvent mounted=${event.mount} currentVehicle=${event.entity.vehicle}")
            if (event.entity is Player) {
                car.trackedRide!!.onMounted(car, event.entity as Player, event.mount)
            }
        }

        @EventHandler
        fun onEntityDismountEvent(event: EntityDismountEvent) {
            if (debug)
                Logger.debug("EntityDismountEvent dismounted=${event.dismounted} currentVehicle=${event.entity.vehicle}")
            val player = event.entity as? Player ?: return
            if (allowUnderwater && !car.attachedTrain.isEjecting && event.entity.isInWater && !player.isSneaking && event.isCancellable) {
                event.isCancelled = true
                return
            }
            car.trackedRide!!.onDismounted(car, player, event.dismounted)
        }

        @EventHandler
        fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
            if (debug)
                Logger.debug("PlayerInteractAtEntityEvent")
            if (car.attachedTrain.trackedRide.requestEnter(event.player, car, entity)) {
//                Logger.debug("Entered")
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
            if (debug)
                Logger.debug("PlayerInteractEntityEvent")
            if (car.attachedTrain.trackedRide.requestEnter(event.player, car, entity)) {
                event.isCancelled = true
            }
        }
    }

    class CarModelListener(val entity: Entity, val car: RideCar) : Listener {
        var debug = false

        @EventHandler
        fun onEntityDamage(event: EntityDamageEvent) {
            if (debug)
                Logger.debug("EntityDamageEvent")
            event.isCancelled = true
        }

        @EventHandler
        fun onEntityDamage(event: EntityDamageByEntityEvent) {
            if (debug)
                Logger.debug("EntityDamageByEntityEvent")
            event.isCancelled = true
        }

        @EventHandler
        fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
            if (debug)
                Logger.debug("PlayerArmorStandManipulateEvent")
            event.isCancelled = true
        }

        @EventHandler
        fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
            if (debug)
                Logger.debug("PlayerInteractAtEntityEvent")
            event.isCancelled = true
        }

        @EventHandler
        fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
            if (debug)
                Logger.debug("PlayerInteractEntityEvent")
            event.isCancelled = true
        }
    }
}