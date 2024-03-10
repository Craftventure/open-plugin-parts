package net.craftventure.audioserver.spatial

import net.craftventure.audioserver.event.AudioServerConnectedEvent
import net.craftventure.audioserver.event.AudioServerDisconnectedEvent
import net.craftventure.bukkit.ktx.event.AsyncPlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

class SpatialAudioManager private constructor() : Listener {
    private val areas = ConcurrentHashMap.newKeySet<SpatialAudio>()

    init {
        Bukkit.getServer().pluginManager.registerEvents(this, PluginProvider.getInstance())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAudioConnect(event: AudioServerConnectedEvent) {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.SPATIAL_SOUNDS)) return
//        Logger.debug("onAudioConnect ${areas.size}")
        areas.forEach {
            it.updateFor(event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAudioDisconnect(event: AudioServerDisconnectedEvent) {
//        Logger.debug("onAudioDisconnect ${areas.size}")
        areas.forEach {
            it.updateFor(event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
//        Logger.debug("onPlayerQuit ${areas.size}")
        areas.forEach {
            it.updateFor(event.player)
            it.cleanupPlayers()
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerLocationChanged(event: AsyncPlayerLocationChangedEvent) {
//        Logger.debug("onPlayerLocationChanged ${areas.size}")
        if (event.locationChanged) {
            if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.SPATIAL_SOUNDS)) return
            if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.AUDIOSERVER_TRACKING)) return
            areas.forEach {
                it.updateFor(event.player, event.to)
            }
        }
    }

    fun register(audio: SpatialAudio) {
        areas.add(audio)
    }

    fun unregister(audio: SpatialAudio) {
        areas.remove(audio)
    }

    companion object {
        val instance by lazy { SpatialAudioManager() }
    }
}