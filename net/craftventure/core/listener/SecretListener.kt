package net.craftventure.core.listener

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class SecretListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val location = block.location

        if (location.blockX == -181 && location.blockY == 50 && location.blockZ == -453) {
            val spawn = Location(location.world, -175.5, 50.0, -457.5, -136f, -25f)
            spawn.world?.playSound(spawn, Sound.ENTITY_TNT_PRIMED, 1f, 0.5f)

            val task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                event.player.teleport(spawn)
            }, 1L, 1L)

            Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
                Bukkit.getScheduler().cancelTask(task)

                spawn.world?.playSound(spawn, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f)

                val force = spawn.direction.normalize().multiply(5)
                event.player.teleport(spawn)
                event.player.velocity = force
                event.player.isFlying = false
                event.player.isGliding = false
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(
                        event.player.uniqueId,
                        "singapore_cannon"
                    )
                }
//                if (!event.player.isVIP())
                executeSync(20 + 20) {
                    event.player.health = 0.0
                    event.player.sendMessage(CVTextColor.serverNotice + "Death by G-force impact...")
                }
            }, 20 * 3L)
        }
    }
}