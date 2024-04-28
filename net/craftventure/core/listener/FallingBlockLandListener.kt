package net.craftventure.core.listener

import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent


class FallingBlockLandListener : Listener {

    @EventHandler
    fun onFallingBlockLand(event: EntityChangeBlockEvent) {
        if (event.entity is FallingBlock) {
            val fallingBlock = event.entity as FallingBlock
            //TODO: FIX13
            if (!fallingBlock.blockData.material.isSolid) {
                fallingBlock.dropItem = false
                fallingBlock.remove()
                event.isCancelled = true
            }
        }
    }
}
