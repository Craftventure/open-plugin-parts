package net.craftventure.core.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.core.metadata.AfkStatus
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class AfkListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerChat(event: AsyncChatEvent) {
        event.player.getMetadata<AfkStatus>()?.updateLastActivity()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerLocationChangedEvent) {
        if (event.locationChanged)
            event.player.getMetadata<AfkStatus>()?.locationUpdated(event.to)
    }
}
