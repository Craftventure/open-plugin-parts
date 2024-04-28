package net.craftventure.core.manager

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.craftventure.bukkit.ktx.entitymeta.EntityEvents.registerListenerAsLongAsValid
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener


object ProjectileEvents {
    fun Entity.removeUponEnteringBubbleColumn() {
        registerListenerAsLongAsValid(object : Listener {
            @EventHandler
            fun tick(event: ServerTickEndEvent) {
                if (isInBubbleColumn) {
                    remove()
                }
            }
        })
    }
}
