package net.craftventure.core.npc.tracker

import net.craftventure.core.ktx.util.Logger.capture
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

object EntitySpawnTrackerManager {
    private val trackers = Collections.newSetFromMap(ConcurrentHashMap<NpcEntityTracker, Boolean>())

    @JvmStatic
    fun unload() {
        synchronized(trackers) {
            try {
                HashSet(trackers).forEach(Consumer { obj: NpcEntityTracker -> obj.release() })
            } catch (e: Exception) {
                capture(e)
            }
        }
    }

    @JvmStatic
    fun add(tracker: NpcEntityTracker) {
        synchronized(trackers) { if (!trackers.contains(tracker)) trackers.add(tracker) }
    }

    fun onEntityDestroyTo(destroyedEntityId: Int, player: Player) {
        synchronized(trackers) {
            try {
                for (tracker in trackers) {
                    if (tracker is PlayerCoupledEntityTracker) {
                        tracker.onEntityDestroyTo(destroyedEntityId, player)
                    }
                }
            } catch (e: Exception) {
                capture(e)
            }
        }
    }

    fun onEntitySpawnTo(spawnedEntityId: Int, player: Player) {
        synchronized(trackers) {
            try {
                for (tracker in trackers) {
                    if (tracker is PlayerCoupledEntityTracker) {
                        tracker.onEntitySpawnTo(spawnedEntityId, player)
                    }
                }
            } catch (e: Exception) {
                capture(e)
            }
        }
    }

    @JvmStatic
    fun remove(tracker: NpcEntityTracker) {
        trackers.remove(tracker)
    }
}