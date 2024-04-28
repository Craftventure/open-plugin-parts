package net.craftventure.core.serverevent

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class PacketPlayerSteerEvent(
    val player: Player,
    val vehicle: Entity,
    val sideways: Float,
    val forwards: Float,
    val isJumping: Boolean,
    val isDismounting: Boolean
) : Event(!Bukkit.isPrimaryThread()), Cancellable {
    private var cancelled: Boolean = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
