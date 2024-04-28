package net.craftventure.core.serverevent

import net.craftventure.core.script.ScriptController
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class ScriptStartEvent(
    val script: ScriptController,
    val group: String,
    val name: String
) : Event(!Bukkit.isPrimaryThread()) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
