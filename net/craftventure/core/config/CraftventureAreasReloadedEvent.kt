package net.craftventure.core.config

import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class CraftventureAreasReloadedEvent : Event() {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
