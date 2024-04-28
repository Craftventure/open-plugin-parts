package net.craftventure.bukkit.ktx.area

import net.craftventure.bukkit.ktx.manager.TrackerAreaManager
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.*
import org.bukkit.event.entity.EntityPoseChangeEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import java.util.concurrent.ConcurrentHashMap

open class AreaTracker(area: Area) : Listener, TrackerAreaManager.AreaListener {
    final override var area = area
        private set
    private val players = ConcurrentHashMap.newKeySet<Player>()
    private val listeners = mutableListOf<StateListener>()

    fun updateArea(area: Area) {
        TrackerAreaManager.unregisterTracker(this)
        this.area = area
        TrackerAreaManager.registerTracker(this)
        Bukkit.getOnlinePlayers().forEach { update(it, it.location) }
    }

    fun addListener(stateListener: StateListener): AreaTracker {
        listeners.remove(stateListener)
        listeners.add(stateListener)
        return this
    }

    fun removeListener(stateListener: StateListener): AreaTracker {
        listeners.remove(stateListener)
        return this
    }

    override fun update(player: Player, location: Location, cancellable: Cancellable?) {
        val inArea = players.contains(player)
        val nowInArea = area.isInArea(location) && player.isConnected()
        if (inArea != nowInArea) {
            if (nowInArea) {
                players.add(player)
                for (i in listeners.indices) {
                    val listener = listeners[i]
                    listener.onEnter(this, player)
                }
            } else if (inArea) {
                players.remove(player)
                for (i in listeners.indices) {
                    val listener = listeners[i]
                    listener.onLeave(this, player)
                }
            }
        } else if (nowInArea) {
            for (i in listeners.indices) {
                val listener = listeners[i]
                listener.onMove(this, player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPoseChange(event: EntityPoseChangeEvent) {
        val player = event.entity as? Player ?: return
        if (player !in players) return
        listeners.forEach { it.onPoseChanged(this, player, event.pose) }
    }

    @EventHandler
    fun onElytraToggle(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return
        if (player !in players) return
        if (event.isGliding) {
            listeners.forEach { it.onStartGliding(this, player, event) }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        if (event.player !in players) return
        listeners.forEach { it.onFlightToggled(this, event.player, event.isFlying) }
    }

    override fun handleLogout(player: Player) {
        players.remove(player)
        for (i in listeners.indices) {
            val listener = listeners[i]
            listener.onLeave(this, player)
        }
    }

    fun start(): AreaTracker {
        Bukkit.getServer().pluginManager.registerEvents(this, PluginProvider.getInstance())
        TrackerAreaManager.registerTracker(this)
        return this
    }

    fun stop(): AreaTracker {
        TrackerAreaManager.unregisterTracker(this)
        HandlerList.unregisterAll(this)
        players.clear()
        return this
    }

    interface StateListener {
        fun onPoseChanged(areaTracker: AreaTracker, player: Player, pose: Pose) {}
        fun onEnter(areaTracker: AreaTracker, player: Player) {}
        fun onLeave(areaTracker: AreaTracker, player: Player) {}
        fun onMove(areaTracker: AreaTracker, player: Player) {}
        fun onFlightToggled(areaTracker: AreaTracker, player: Player, flying: Boolean) {}
        fun onStartGliding(areaTracker: AreaTracker, player: Player, cancellable: Cancellable) {}
    }
}
