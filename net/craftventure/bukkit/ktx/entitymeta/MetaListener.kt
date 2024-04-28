package net.craftventure.bukkit.ktx.entitymeta

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class MetaListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityRemoved(event: EntityRemoveFromWorldEvent) {
//        Logger.debug("Removed ${event.entity.type}")
        if (event.entity !is Player) {
            MetaAnnotations.cleanup(event.entity)
            EntityEvents.cleanup(event.entity)
        }
    }
}