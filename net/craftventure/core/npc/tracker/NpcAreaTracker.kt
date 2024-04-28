package net.craftventure.core.npc.tracker

import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.Area.Companion.chunkDifference
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.manager.TrackerAreaManager
import net.craftventure.bukkit.ktx.manager.TrackerAreaManager.updateTracker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable

class NpcAreaTracker(override var area: Area) : NpcEntityTracker(), TrackerAreaManager.AreaListener {
    var isAutomaticJoin = true

    val world: World
        get() = area.world

    fun updateArea(area: Area) {
        val difference = chunkDifference(this.area, area)
        updateTracker(difference, this)
        this.area = area
        Bukkit.getOnlinePlayers().forEach { update(it, it.location) }
    }

    override fun update(player: Player, location: Location, cancellable: Cancellable?) {
//        logcat { "Update for ${player.name} at ${location.toVector().asString()}" }
        if (!player.isConnected()) {
            removePlayer(player)
            return
        }
        if (!area.isInArea(location)) {
            if (player in players) {
                for (npcEntity in npcs) {
                    npcEntity.destroy(player)
                }
                removePlayer(player)
//                logcat { "Removing ${player.name} with entities ${npcEntities.size}" }
            }
        } else if (isAutomaticJoin) {
            if (area.isInArea(location) && !players.contains(player)) {
                addPlayer(player)
            }
        }
    }

    override fun handleLogout(player: Player) {
        removePlayer(player)
    }

    override fun onStartTracking() {
        super.onStartTracking()
        TrackerAreaManager.registerTracker(this)
    }

    override fun onStopTracking() {
        super.onStopTracking()
        TrackerAreaManager.unregisterTracker(this)
        clearPlayers()
    }

    init {
        EntitySpawnTrackerManager.add(this)
    }
}