package net.craftventure.bukkit.ktx.event

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent


class PlayerLocationChangedEvent(
    player: Player,
    val from: Location,
    val to: Location,
    val locationChanged: Boolean,
    val lookChanged: Boolean,
    val isTeleport: Boolean
) : PlayerEvent(player), Cancellable {
    private var cancelled: Boolean = false
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun isCancelled(): Boolean = cancelled

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
