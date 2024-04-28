package net.craftventure.core.listener

import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent


class BarrierListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val handType = event.player.inventory.itemInMainHand.type
        if ((event.block.type == Material.BARRIER && handType != Material.BARRIER) || (event.block.type != Material.BARRIER && handType == Material.BARRIER)) {
            event.isCancelled = true
            event.block.location.clone().add(0.5, 0.5, 0.5)
                .spawnParticleX(
                    Particle.BLOCK_MARKER,
                    players = setOf(event.player),
                    data = Material.BARRIER.createBlockData()
                )
        }
    }
}
