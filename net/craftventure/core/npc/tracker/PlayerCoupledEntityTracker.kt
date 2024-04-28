package net.craftventure.core.npc.tracker

import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.core.CraftventureCore
import net.minecraft.server.level.ChunkMap
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable

class PlayerCoupledEntityTracker(
    private val player: Player,
    private val includePlayerSelf: Boolean,
) : NpcEntityTracker(), Listener {
    private var entityTracker: ChunkMap.TrackedEntity? = null

    private var bukkitRunnable: BukkitRunnable? = null

    private fun generateRunnable() = object : BukkitRunnable() {
        override fun run() {
            if (!hasStarted) return
            players.toList().forEach {
                if (!it.isConnected())
                    removePlayer(it)
            }
        }
    }

    fun onEntityDestroyTo(destroyedEntityId: Int, player: Player) {
        if (destroyedEntityId == this.player.entityId) {
            synchronized(players) {
                removePlayer(player)
            }
        }
    }

    fun onEntitySpawnTo(spawnedEntityId: Int, player: Player) {
        if (player === this.player) return
        if (spawnedEntityId == this.player.entityId) {
            synchronized(players) {
                addPlayer(player)
            }
        }
    }

    override fun onStartTracking() {
        super.onStartTracking()
        if (entityTracker == null) {
            val tracker = (player.world as CraftWorld).handle.chunkSource.chunkMap
            entityTracker = tracker.entityMap[player.entityId]
        }
        if (entityTracker != null) {
            addPlayers(*entityTracker!!.seenBy.stream()
                .filter { it.player != null }
                .map { it.player.bukkitEntity as Player }
                .toList().toTypedArray())
        }
        if (includePlayerSelf) addPlayer(player)
        for (player in players) {
            for (npcEntity in npcs) {
                npcEntity.spawn(player)
            }
        }
        bukkitRunnable = generateRunnable()
        bukkitRunnable!!.runTaskTimer(CraftventureCore.getInstance(), 1L, 20L)
    }

    override fun onStopTracking() {
        super.onStopTracking()
        bukkitRunnable?.cancel()
        bukkitRunnable = null
        clearPlayers()
    }
}