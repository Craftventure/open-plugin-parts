package net.craftventure.core.feature.kart.actions

import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.metadata.setLeaveLocation
import org.bukkit.entity.Player

class ExitToSafeLocation : ExitHandler {
    override fun onExit(kart: Kart, player: Player) {
//        logcat { "Handle kart exit for ${player.name}" }
        //                Logger.debug("Exit handler to kart loc")
        val location = player.location.clone()
        location.set(kart.location)
        player.setLeaveLocation(location)
//        player.teleport(location)
    }
}