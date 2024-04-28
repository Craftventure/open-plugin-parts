package net.craftventure.core.feature.kart.actions

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.metadata.setLeaveLocation
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.getNearestWarpFromLocation
import net.craftventure.database.bukkit.extensions.toLocation
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent

open class ExitToNearestWarp : ExitHandler {
    override fun onExit(kart: Kart, player: Player) {
        val location = player.location
        val warp = MainRepositoryProvider.warpRepository.getNearestWarpFromLocation(location, player)
        //                    Logger.debug("Exit handler to warp")

        player.setLeaveLocation(
            warp?.toLocation()
                ?: location.world!!.spawnLocation
        )
//        player.teleport(
//            warp?.toLocation()
//                ?: location.world!!.spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN
//        )
        player.sendMessage(CVTextColor.serverNotice + "The kart ejected you to the nearest warp")
    }
}