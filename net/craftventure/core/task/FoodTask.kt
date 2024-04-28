package net.craftventure.core.task

import net.craftventure.core.CraftventureCore
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.event.entity.EntityRegainHealthEvent
import kotlin.math.min


object FoodTask {
    private var isActive = false
    private var lastUpdateTime = System.currentTimeMillis()

    fun init() {
        if (isActive)
            return
        isActive = true

        lastUpdateTime = System.currentTimeMillis()

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            Bukkit.getOnlinePlayers().forEach { player ->
                if (!player.isValid) return@forEach

                val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
                if (player.health < maxHealth) {
//                    logcat { "Heal ${player.name}" }
                    val newHealth = min(player.health + 1.0, maxHealth)
                    val event = EntityRegainHealthEvent(player, 1.0, EntityRegainHealthEvent.RegainReason.CUSTOM)
                    Bukkit.getPluginManager().callEvent(event)
                    if (event.isCancelled) return@forEach

                    player.health = newHealth
                    player.sendHealthUpdate()
                }
            }

        }, 80, 80)
    }
}
