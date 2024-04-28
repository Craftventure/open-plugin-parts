package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.core.async.executeSync
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerGameModeChangeEvent

class GameModeChangeListener : Listener {
    @EventHandler
    fun onGameModeChanged(event: PlayerGameModeChangeEvent) {
        executeSync {
            MessageBarManager.trigger(event.player)
        }
    }
}