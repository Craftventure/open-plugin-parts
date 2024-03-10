package net.craftventure.core.ride.operator

import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.extensions.getAudioChannelMeta
import net.craftventure.audioserver.packet.PacketOperatorControlUpdate
import net.craftventure.audioserver.packet.PacketOperatorDefinition
import net.craftventure.audioserver.packet.PacketOperatorRideUpdate
import net.craftventure.audioserver.packet.PacketOperatorSlotUpdate
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.inventory.impl.RideOperatorMenu
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.metadata.CvMetadata
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.trackedride.FlatrideManager
import net.craftventure.core.ride.trackedride.TrackedRideManager
import net.craftventure.core.ride.trackedride.TracklessRideManager
import net.craftventure.core.utils.OperatorUtils
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.collections.set


class OperatorManager {
    private var initialised = false
    private val rideOperatorMenuMap = HashMap<String, RideOperatorMenu>()
    var controlInvalidatedListener: Set<ControlInvalidatedListener> = setOf()
    var operatorInvalidatedListener: Set<OperatorInvalidatedListener> = setOf()

    init {
        initialise()
    }

    val operablerides: List<OperableRide>
        get() {
            val operateableRides = ArrayList<OperableRide>()
            for (flatride in FlatrideManager.getFlatrideList()) {
                if (flatride is OperableRide) {
                    if (flatride.ride != null)
                        operateableRides.add(flatride as OperableRide)
                }
            }

            for (trackedRide in TrackedRideManager.getTrackedRideList()) {
                if (trackedRide is OperableRide) {
                    if (trackedRide.ride != null)
                        operateableRides.add(trackedRide as OperableRide)
                }
            }

            for (tracklessRide in TracklessRideManager.getRideList()) {
                if (tracklessRide is OperableRide) {
                    if (tracklessRide.ride != null)
                        operateableRides.add(tracklessRide as OperableRide)
                }
            }
            return operateableRides
        }

    fun updateForcedVisibility(
        operableRide: OperableRide,
        player: Player,
        location: Location = player.location,
        visible: Boolean = operableRide.isInOperateableArea(location)
    ) {
        if (!OperatorUtils.isAllowedToOperate(operableRide, player)) return
        val audioMeta = player.getAudioChannelMeta() ?: return
        val updatePacket = PacketOperatorRideUpdate(
            PacketOperatorRideUpdate.RideUpdate(
                operableRide.id,
                operableRide.ride?.displayName,
                visible
            )
        )
        updatePacket.send(audioMeta)
    }

    fun broadcastRideOperated(operableRide: OperableRide) {
        val lastBroadcast = lastOperatorBroadcasts[operableRide]
        if (lastBroadcast == null || lastBroadcast < System.currentTimeMillis() - 60 * 3 * 1000) {
            lastOperatorBroadcasts[operableRide] = System.currentTimeMillis()
            val ride = operableRide.ride
            val operator = operableRide.getOperatorForSlot(0) ?: return

            var component: Component = Component.text(
                ride!!.displayName + " is now being operated by " + operator.name,
                CVTextColor.serverNotice
            )

            if (ride.warpId != null) {
                component += Component.text(" Click here to warp")
                    .color(CVTextColor.serverNoticeAccent)
                    .hoverEvent(Component.text("Click here to warp", CVTextColor.CHAT_HOVER).asHoverEvent())
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/warp ${ride.warpId}"))
            }

            Bukkit.getServer().sendMessage(component)
        }
    }

    private fun initialise() {
        if (initialised)
            return

        initialised = true
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {

            val iterator = rideOperatorMenuMap.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val rideOperatorMenu = entry.value
                if (rideOperatorMenu.hasViewers()) {
                    rideOperatorMenu.updateOperatorControls()
                } else {
//                    rideOperatorMenu.remove()
                    iterator.remove()
                }
            }

            for (operateableRide in operablerides) {
                for (operatorControl in operateableRide.provideControls()) {
                    if (operatorControl.isInvalidated) {
                        controlInvalidatedListener.forEach {
                            it.onControlUpdated(operateableRide, operatorControl)
                        }
                        //                            Logger.console("Updating control %s", operatorControl.getId());
                        val packetOperatorControlUpdate =
                            PacketOperatorControlUpdate(operatorControl.toModel(operateableRide.id))
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (PermissionChecker.isVIP(player)) {
                                AudioServer.instance.audioServer?.sendPacket(player, packetOperatorControlUpdate)
                            }
                        }
                    }
                }
            }

            for (operateableRide in operablerides) {
                for (i in 0..operateableRide.totalOperatorSpots) {
                    val player = operateableRide.getOperatorForSlot(i)
                    if (player != null) {
                        if (!player.isConnected()) {
                            cancelOperating(operateableRide, player)
                            //                                operateableRide.cancelOperating(0);
                            continue
                        } else if (!operateableRide.isInOperateableArea(player.location)) {
//                            Logger.info("Player left area")
                            cancelOperating(operateableRide, player)
                            //                                operateableRide.cancelOperating(0);
                            continue
                        } else {
                            val cvMetadata = player.getMetadata<CvMetadata>()
                            if (cvMetadata != null) {
                                if (cvMetadata.afkStatus.isAfk) {
                                    cancelOperating(operateableRide, player)
                                    //                                        operateableRide.cancelOperating(0);
                                    continue
                                }
                            }
                        }
                    }
                }

                for (operatorControl in operateableRide.provideControls()) {
                    operatorControl.update()
                }

                if (operateableRide.isBeingOperated) {
                    operateableRide.updateWhileOperated()
                }
            }

        }, 1L, 1L)
    }

    fun getOpereableRideById(id: String): OperableRide? {
        for (operateableRide in operablerides) {
            if (operateableRide.ride?.name == id) {
                return operateableRide
            }
        }
        return null
    }

    fun getOperatingRide(player: Player): OperableRide? {
        for (operateableRide in operablerides) {
            for (i in 0 until operateableRide.totalOperatorSpots) {
                if (operateableRide.getOperatorForSlot(i) === player) {
                    return operateableRide
                }
            }
        }
        return null
    }

    fun isOperatingSomewhere(player: Player): Boolean {
        for (operateableRide in operablerides) {
            for (i in 0 until operateableRide.totalOperatorSpots) {
                if (operateableRide.getOperatorForSlot(i) === player) {
                    return true
                }
            }
        }
        return false
    }

    fun startOperating(operableRide: OperableRide, player: Player, slot: Int): Boolean {
//        Logger.debug(
//            "operableRide.getRide()=${operableRide.getRide()} inArea=${operableRide.isInOperateableArea(player.location)} operable=${operableRide.getRide()!!.state.isOperable} allowed=${OperatorUtils.isAllowedToOperate(
//                operableRide,
//                player
//            )}"
//        )

        if (player.isInsideVehicle) {
//            Logger.debug("vehicle")
            return false
        }
        if (!OperatorUtils.isAllowedToOperate(operableRide, player)) {
//            Logger.debug("not allowed")
            return false
        }
        if (isOperatingSomewhere(player)) {
//            Logger.debug("already opping")
            return false
        }
        val cvMetadata = player.getMetadata<CvMetadata>()
        if (cvMetadata != null)
            if (cvMetadata.afkStatus.isAfk) {
//                Logger.debug("afk")
                return false
            }

        if (operableRide.ride == null || (!player.isCrew() && !operableRide.ride!!.state!!.isOperable)) {
//            Logger.debug("no ride or not operable")
            return false
        }

        if (operableRide.isInOperateableArea(player.location)) {
//            Logger.debug("In OP area")
            if (player.isCrew()) {
                operableRide.cancelOperating(slot)
            }
            if (operableRide.setOperator(player, slot)) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "operator")
                }

                operatorInvalidatedListener.forEach {
                    it.onOperatorUpdated(operableRide, slot, player)
                }

                val packetOperatorControlUpdate = PacketOperatorSlotUpdate(
                    PacketOperatorDefinition.OperatorSlot(
                        operableRide.id, slot, player.uniqueId.toString(), player.name
                    )
                )
                for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                    if (PermissionChecker.isVIP(onlinePlayer)) {
                        AudioServer.instance.audioServer?.sendPacket(onlinePlayer, packetOperatorControlUpdate)
                    }
                }

//                broadcastRideOperated(operateableRide)
                player.gameState()?.operatingRide = operableRide
                return true
            }
//            Logger.debug("Failed to set")
        }
//        Logger.debug("Failed to set rideop")
        return false
    }

    fun cancelOperating(operableRide: OperableRide, player: Player): Boolean {
        for (i in 0 until operableRide.totalOperatorSpots) {
            if (operableRide.getOperatorForSlot(i) === player) {
                operableRide.cancelOperating(i)
                val packetOperatorControlUpdate = PacketOperatorSlotUpdate(
                    PacketOperatorDefinition.OperatorSlot(
                        operableRide.id, i, null, null
                    )
                )
                operatorInvalidatedListener.forEach {
                    it.onOperatorUpdated(operableRide, i, null)
                }
                for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                    if (PermissionChecker.isVIP(onlinePlayer)) {
                        AudioServer.instance.audioServer?.sendPacket(onlinePlayer, packetOperatorControlUpdate)
                    }
                }
                player.gameState()?.operatingRide = null
                return true
            }
        }
        return false
        //        PacketOperatorControlUpdate packetOperatorControlUpdate = new PacketOperatorControlUpdate(OperatorControlModel.of(operateableRide.getId(), operatorControl));
        //        for (Player player : Bukkit.getOnlinePlayers()) {
        //            if (Permissions.isVIP(player)) {
        //                AudioServer.getInstance().getAudioServer().sendPacket(player, packetOperatorControlUpdate);
        //            }
        //        }
    }

    fun clicked(player: Player, rideId: String, controlId: String) {
        if (!PluginProvider.isOnMainThread()) {
            Bukkit.getScheduler()
                .scheduleSyncDelayedTask(CraftventureCore.getInstance()) { clicked(player, rideId, controlId) }
            return
        }

        for (operateableRide in operablerides) {
            val ride = operateableRide.ride
            if (ride != null) {
                if (rideId == ride.name) {
                    for (slot in 0 until operateableRide.totalOperatorSpots) {
                        if (controlId == slot.toString()) {
                            val currentOperator = operateableRide.getOperatorForSlot(slot)
                            if (currentOperator === player) {
                                CraftventureCore.getOperatorManager().cancelOperating(operateableRide, player)
                            } else {
                                CraftventureCore.getOperatorManager().startOperating(operateableRide, player, slot)
                            }
                            return
                        }
                    }
                    if (player.isCrew() || operateableRide.getOperatorSlot(player) >= 0) {
                        for (operatorControl in operateableRide.provideControls()) {
                            if (controlId == operatorControl.id) {
                                if (operatorControl.click(operateableRide, player)) {
//                                    Logger.info("${player.name} used operator button $controlId from $rideId")
                                    val cvMetadata = player.getMetadata<CvMetadata>()
                                    cvMetadata?.afkStatus?.updateLastOperatorActivity()
                                }
                                return
                            }
                        }
                    }
                }
            }
        }
        Logger.debug("Failed to resolve click by ${player.name} at $rideId for $controlId")
    }

    @Deprecated("")
    fun openMenu(player: Player, rideName: String): Boolean {
        for (operateableRide in operablerides) {
            val ride = operateableRide.ride
            if (ride != null && ride.name == rideName) {
                val rideOperatorMenu =
                    rideOperatorMenuMap.getOrPut(operateableRide.id, { RideOperatorMenu(operateableRide) })
                rideOperatorMenu.openAsMenu(player)
                return true
            }
        }

        return false
    }

    fun openMenu(player: Player, operableRide: OperableRide) {
        val rideOperatorMenu = rideOperatorMenuMap.getOrPut(operableRide.id, { RideOperatorMenu(operableRide) })
        rideOperatorMenu.openAsMenu(player)
    }

    interface ControlInvalidatedListener {
        fun onControlUpdated(ride: OperableRide, operatorControl: OperatorControl)
    }

    interface OperatorInvalidatedListener {
        fun onOperatorUpdated(ride: OperableRide, slot: Int, player: Player?)
    }

    companion object {
        const val INTERACTION_TIMEOUT: Long = 300
        private val lastOperatorBroadcasts = HashMap<OperableRide, Long>()
    }
}
