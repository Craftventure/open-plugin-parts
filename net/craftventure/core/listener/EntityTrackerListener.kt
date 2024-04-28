package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.core.metadata.EntityDamageTrackerMeta
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityRegainHealthEvent

class EntityTrackerListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        event.entity.getMetadata<EntityDamageTrackerMeta>()?.apply {
            onDamaged(player, event.damage)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        (event.entity as? LivingEntity)?.apply {
            val maxHealth = Attribute.GENERIC_MAX_HEALTH
            if (health + event.amount >= this.getAttribute(maxHealth)!!.value) {
                event.entity.getMetadata<EntityDamageTrackerMeta>()?.reset()
            }
        }
    }
}