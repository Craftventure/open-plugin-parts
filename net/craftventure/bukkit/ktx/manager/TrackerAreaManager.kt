package net.craftventure.bukkit.ktx.manager

import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.concurrent.ConcurrentHashMap

object TrackerAreaManager {
    private val areaMap = ConcurrentHashMap<Long, MutableSet<AreaListener>>()

    interface AreaListener {
        val area: Area

        fun update(player: Player, location: Location, cancellable: Cancellable? = null)

        fun handleLogout(player: Player)
    }

    private val listener = object : org.bukkit.event.Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onPlayerMoveMonitor(event: PlayerLocationChangedEvent) {
            if (event.locationChanged)
                update(event.player, event.to, event)
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onPlayerTeleportMonitor(event: PlayerTeleportEvent) {
            update(event.player, event.to, event)
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onPlayerQuit(event: PlayerQuitEvent) {
            logout(event.player)
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onPlayerJoin(event: PlayerJoinEvent) {
            update(event.player, event.player.location, null)
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onPlayerRespawn(event: PlayerRespawnEvent) {
            update(event.player, event.respawnLocation, null)
        }
    }

    private fun logout(player: Player) {
        lookup(player.chunk).forEach { it.handleLogout(player) }
    }

    private fun update(player: Player, to: Location, cancellable: Cancellable?) {
        lookup(player.chunk).forEach { it.update(player, to, cancellable) }
        if (to.chunk.chunkKey != player.chunk.chunkKey)
            lookup(to.chunk).forEach { it.update(player, to, cancellable) }
    }

    fun lookup(chunk: Chunk) = areaMap[chunk.chunkKey] ?: emptySet()

    private fun getSetFor(chunkKey: Long) = areaMap.getOrPut(chunkKey) { ConcurrentHashMap.newKeySet() }
    private fun getSetFor(chunk: Chunk) = getSetFor(chunk.chunkKey)

    fun debug(player: Player) {
        player.sendMessage(CVTextColor.serverNotice + "Total chunks: ${areaMap.size}")
        player.sendMessage(CVTextColor.serverNotice + "Areas in player chunk: ${lookup(player.chunk).size}")
    }

    fun registerTracker(tracker: AreaListener) {
        val area = tracker.area
        area.getSpannedChunks().forEach { chunk ->
            getSetFor(chunk).add(tracker)
            chunk.entities.filterIsInstance<Player>().forEach { tracker.update(it, it.location) }
        }
    }

    fun updateTracker(difference: Area.Companion.ChunkDifference, tracker: AreaListener) {
        difference.removed.forEach {
//            logcat { "Removed $it" }
            getSetFor(it).remove(tracker)
        }
        difference.added.forEach {
//            logcat { "Added $it" }
            getSetFor(it).add(tracker)
        }
    }

    fun unregisterTracker(tracker: AreaListener) {
        val area = tracker.area
        area.getSpannedChunks().forEach { chunk ->
            getSetFor(chunk).remove(tracker)
        }
    }

    private var started = false

    @JvmStatic
    fun start() {
        if (started) return
        Bukkit.getServer().pluginManager.registerEvents(listener, PluginProvider.getInstance())
        started = true
    }

    @JvmStatic
    fun stop() {
        if (!started) return
        HandlerList.unregisterAll(listener)
        started = false
    }
}