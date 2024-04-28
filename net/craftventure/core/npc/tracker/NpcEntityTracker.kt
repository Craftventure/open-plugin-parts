package net.craftventure.core.npc.tracker

import net.craftventure.core.npc.NpcEntity
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

abstract class NpcEntityTracker {
    protected var hasReleased = false
        private set
    protected var hasStarted = false
        private set

    private val trackedPlayersInternal: MutableSet<Player> = mutableSetOf()
    val players: Set<Player> get() = trackedPlayersInternal

    private val trackedNpcsInternal: MutableSet<NpcEntity> = ConcurrentHashMap.newKeySet()
    val npcs: Set<NpcEntity> get() = trackedNpcsInternal

    fun addPlayer(player: Player) {
        if (trackedPlayersInternal.add(player)) {
            for (npcEntity in npcs) {
                npcEntity.spawn(player)
                listeners.forEach { it.onSpawnToPlayer(player, npcEntity) }
            }
            onAdded(player)
        }
    }

    fun addPlayers(vararg players: Player) {
        players.forEach { player -> addPlayer(player) }
    }

    fun removePlayer(player: Player) {
        if (trackedPlayersInternal.remove(player)) {
//            if (player.isConnected())
            for (npcEntity in npcs) {
                npcEntity.destroy(player)
                listeners.forEach { it.onDespawnToPlayer(player, npcEntity) }
            }
            onRemoved(player)
        }
    }

    protected fun clearPlayers() {
        for (player in players.toList()) {
            removePlayer(player)
        }
    }

    protected fun clearNpcs() {
        for (npcEntity in npcs.toList()) {
            removeEntity(npcEntity)
        }
    }

    open fun addEntity(npcEntity: NpcEntity) {
        if (trackedNpcsInternal.add(npcEntity)) {
            npcEntity.setEntityTracker(this)
            for (player in players) {
                npcEntity.spawn(player)
                listeners.forEach { it.onSpawnToPlayer(player, npcEntity) }
            }
        }
    }

    open fun removeEntity(npcEntity: NpcEntity) {
        if (trackedNpcsInternal.remove(npcEntity)) {
            for (player in players) {
                npcEntity.destroy(player)
                listeners.forEach { it.onDespawnToPlayer(player, npcEntity) }
            }
            npcEntity.setEntityTracker(null)
        }
    }

    open fun forceRespawn(player: Player) {
        for (npcEntity in npcs) {
            npcEntity.destroy(player)
            listeners.forEach { it.onDespawnToPlayer(player, npcEntity) }
            npcEntity.spawn(player)
            listeners.forEach { it.onSpawnToPlayer(player, npcEntity) }
        }
    }

    fun startTracking() {
        if (!hasStarted) {
            EntitySpawnTrackerManager.add(this)
            players.forEach { player ->
                npcs.forEach { entity ->
                    entity.spawn(player)
                    listeners.forEach { it.onSpawnToPlayer(player, entity) }
                }
            }
            onStartTracking()
            hasStarted = true
        }
    }

    fun stopTracking() { //        Logger.console("pauseTracking");
        if (hasStarted) {
            clearPlayers()
            EntitySpawnTrackerManager.remove(this)
            onStopTracking()
            hasStarted = false
        }
    }

    fun release() {
        if (!hasReleased) {
            stopTracking()
            clearNpcs()
            clearPlayers()
            onRelease()
            hasReleased = true
        }
    }

    protected open fun onStartTracking() {}
    protected open fun onStopTracking() {}
    protected open fun onRelease() {}

    private val listeners = mutableSetOf<Listener>()

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    protected fun onAdded(player: Player) {
        listeners.forEach { it.onAdded(player) }
    }

    protected fun onRemoved(player: Player) {
        listeners.forEach { it.onRemoved(player) }
    }

    interface Listener {
        fun onAdded(player: Player) {}
        fun onRemoved(player: Player) {}
        fun onSpawnToPlayer(player: Player, entity: NpcEntity) {}
        fun onDespawnToPlayer(player: Player, entity: NpcEntity) {}
    }
}