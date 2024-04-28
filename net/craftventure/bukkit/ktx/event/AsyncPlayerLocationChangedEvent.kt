package net.craftventure.bukkit.ktx.event

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent


class AsyncPlayerLocationChangedEvent(
    player: Player,
    val to: Location,
    val locationChanged: Boolean,
    val lookChanged: Boolean,
    val isTeleport: Boolean
) : PlayerEvent(player, true) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
