package net.craftventure.core.serverevent

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class PlayerHotKeyPressedEvent(
    val player: Player,
    val key: Key
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

    enum class Key {
        Q, F
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
