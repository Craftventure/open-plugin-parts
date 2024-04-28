package net.craftventure.core.serverevent

import net.craftventure.database.type.BankAccountType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class BankAccountUpdateEvent(
    val player: Player,
    val type: BankAccountType,
    val newBalance: Long
) : Event(!Bukkit.isPrimaryThread()) {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
