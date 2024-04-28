package net.craftventure.core.metadata

import net.craftventure.bukkit.ktx.entitymeta.BaseEntityMetadata
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.*

class EntityDamageTrackerMeta : BaseEntityMetadata() {
    private val players = HashSet<PlayerData>()

    override fun debugComponent() = Component.text("players=${players.joinToString { it.uuid.toString() }}")

    fun players() = players.iterator()

    fun reset() {
        players.clear()
    }

    fun onDamaged(player: Player, damage: Double) {
        val data = players.find { it.uuid === player.uniqueId } ?: PlayerData(player.uniqueId)
        data.damage += damage
        players.add(data)
    }

    fun removePlayer(player: Player) {
        players.removeAll { it.uuid == player.uniqueId }
    }

    data class PlayerData(val uuid: UUID) {
        var damage: Double = 0.0
    }
}