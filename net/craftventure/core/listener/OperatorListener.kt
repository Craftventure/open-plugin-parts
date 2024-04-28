package net.craftventure.core.listener

import net.craftventure.audioserver.event.AudioServerConnectedEvent
import net.craftventure.audioserver.packet.PacketOperatorDefinition
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.utils.OperatorUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener


class OperatorListener : Listener {
    @EventHandler
    fun onAudioServerConnected(event: AudioServerConnectedEvent) {
        //        if (Permissions.isCrew(event.getPlayer())) {
        val operateableRides = CraftventureCore.getOperatorManager().operablerides
        val rides = ArrayList<PacketOperatorDefinition.Ride>(operateableRides.size)
        for (operateableRide in operateableRides) {
            if (operateableRide.ride != null) {
                val operatorControlModels = ArrayList<PacketOperatorDefinition.OperatorControlModel>()

                for (operatorControl in operateableRide.provideControls()) {
                    operatorControlModels.add(operatorControl.toModel(operateableRide.id))
                }

                rides.add(createRide(operateableRide, operatorControlModels, event.player))
            }
        }

        val packet = PacketOperatorDefinition(rides)
        packet.send(event.channelMetaData)
        //        }
    }

    fun createRide(
        operableRide: OperableRide,
        controls: List<PacketOperatorDefinition.OperatorControlModel>,
        player: Player
    ) = PacketOperatorDefinition.Ride(
        id = operableRide.id,
        controls = controls,
        name = operableRide.ride?.displayName,
        forceDisplay = operableRide.isInOperateableArea(player.location) && OperatorUtils.isAllowedToOperate(
            operableRide,
            player
        )
    ).apply {
        for (i in 0 until operableRide.totalOperatorSpots) {
            val operator = operableRide.getOperatorForSlot(i)
            operatorSlots.add(
                PacketOperatorDefinition.OperatorSlot(
                    this.id,
                    i,
                    operator?.uniqueId?.toString(),
                    operator?.name
                )
            )
        }
    }
}
