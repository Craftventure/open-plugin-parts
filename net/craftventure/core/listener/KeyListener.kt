package net.craftventure.core.listener

import net.craftventure.core.serverevent.PlayerHotKeyPressedEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class KeyListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val keyEvent = PlayerHotKeyPressedEvent(event.player, PlayerHotKeyPressedEvent.Key.F)
        Bukkit.getPluginManager().callEvent(keyEvent)
        if (keyEvent.isCancelled)
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDropItemEvent(event: PlayerDropItemEvent) {
        val keyEvent = PlayerHotKeyPressedEvent(event.player, PlayerHotKeyPressedEvent.Key.Q)
        Bukkit.getPluginManager().callEvent(keyEvent)
        if (keyEvent.isCancelled)
            event.isCancelled = true
    }
}