package net.craftventure.core.feature.kart.actions

import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.metadata.setLeaveLocation
import org.bukkit.entity.Player

class ExitNonVipToNearestWarp : ExitToNearestWarp() {
    override fun onExit(kart: Kart, player: Player) {
        if (!player.isVIP()) {
            super.onExit(kart, player)
        } else {
            val location = player.location.clone()
            location.set(kart.location)
            player.setLeaveLocation(location)
//            player.teleport(location)
        }
    }
}