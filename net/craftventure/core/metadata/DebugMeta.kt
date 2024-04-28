package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.npc.NpcEntity
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player

class DebugMeta(
    val player: Player
) : BasePlayerMetadata(player) {
    val debugEntities = mutableListOf<NpcEntity>()

    fun clearAllDebugEntities() {
        debugEntities.forEach { it.destroy(player) }
        debugEntities.clear()
    }

    override fun debugComponent() = Component.text("debugEntities=${debugEntities.size}")

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { DebugMeta(player) }
    }

    companion object {
        @JvmOverloads
        @JvmStatic
        fun setLeaveLocation(player: Player, location: Location, applyTeleport: Boolean = false) {
            player.setLeaveLocation(location, applyTeleport)
        }
    }
}