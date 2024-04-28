package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.extension.renewPotionEffect
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffectType


class SkateListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerLocationChangedEvent) {
        if (!event.locationChanged) return
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.SKATES_ENABLED)) return
        if (event.player.isOnGround || event.player.fireTicks > 0) {
            val shoes = event.player.equippedItemsMeta()?.appliedEquippedItems?.bootsItem?.id ?: return
            val location = event.player.location
            if (shoes.startsWith("iceskate")) {
                val type = location.clone().add(0.0, -0.5, 0.0).block.type
                when (type) {
                    Material.ICE, Material.PACKED_ICE, Material.FROSTED_ICE, Material.BLUE_ICE -> {
                        event.player.removePotionEffect(PotionEffectType.SLOW)
                        event.player.renewPotionEffect(PotionEffectType.SPEED, duration = 25, amplifier = 2)
                    }

                    else -> {
                        event.player.removePotionEffect(PotionEffectType.SPEED)
                        event.player.renewPotionEffect(PotionEffectType.SLOW, duration = 25, amplifier = 2)
                    }
                }
            }
        }
    }
}