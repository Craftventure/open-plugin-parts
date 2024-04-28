package net.craftventure.core.serverevent

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class PacketUseEntityEvent(
    val player: Player,
    val interactedEntityId: Int,
    val type: Type,
) : Event(!Bukkit.isPrimaryThread()), Cancellable {
    private var cancelled: Boolean = false

    val isAttacking: Boolean
        get() = type == Type.ATTACK

    val isInteracting: Boolean
        get() = type == Type.INTERACT || type == Type.INTERACT_AT

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    enum class Type {
        INTERACT,
        ATTACK,
        INTERACT_AT
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
