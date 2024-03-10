package net.craftventure.core.feature.shop

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isDisconnected
import net.craftventure.core.feature.shop.dto.NpcFileShopKeeperDto
import net.craftventure.core.manager.TeamsManager
import net.craftventure.core.metadata.PlayerSpecificTeamsMeta
import net.craftventure.core.npc.EntityBitFlags
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.actor.ActorPlayback
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.minusFlag
import net.craftventure.withFlag
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox


data class ShopNpcKeeper(
    val shopPresenter: ShopPresenter,
    val playback: ActorPlayback,
    val entity: NpcEntity = playback.npcEntity!!,
    val config: NpcFileShopKeeperDto,
) {
    private var areaPlayers = hashSetOf<Player>()
    private val boundingBox = EntityMetadata.getBoundingBox(entity.entityType)
    private val boundingBoxAtCurrentLocation
        get() = boundingBox?.let {
            val entityLocation = entity.getLocation()
            BoundingBox(
                entityLocation.x + it.minX,
                entityLocation.y + it.minY,
                entityLocation.z + it.minZ,
                entityLocation.x + it.maxX,
                entityLocation.y + it.maxY,
                entityLocation.z + it.maxZ
            )
        }

    val entityId = entity.entityId

    fun play() {
        playback.play()
    }

    fun stop() {
        playback.stop()
    }

    fun update() {
        areaPlayers.removeAll { it.isDisconnected() }
    }

    fun remove(player: Player): Boolean {
        if (player !in areaPlayers) return false
        areaPlayers.remove(player)
        player.getMetadata<PlayerSpecificTeamsMeta>()?.remove(entity)
        setGlowing(player, false)
        return true
    }

    private fun setGlowing(player: Player, glowing: Boolean) {
        val currentValue = entity.getMetadata(EntityMetadata.Entity.sharedFlags) ?: 0
        val newValue =
            if (glowing) currentValue.withFlag(EntityBitFlags.EntityState.GLOWING) else currentValue.minusFlag(
                EntityBitFlags.EntityState.GLOWING
            )
        entity.setPlayerSpecificMetadata(
            EntityMetadata.Entity.sharedFlags,
            newValue,
            listOf(player)
        )
    }

    fun add(player: Player): Boolean {
        if (player in areaPlayers) return false
        areaPlayers.add(player)
        player.getMetadata<PlayerSpecificTeamsMeta>()
            ?.addOrUpdate(entity, TeamsManager.getTeamDataFor(NamedTextColor.GOLD))
        setGlowing(player, true)
        return true
    }

    fun matches(player: Player): Boolean {
        val boundingBox = this.boundingBoxAtCurrentLocation ?: return false
        val result = boundingBox.rayTrace(
            player.eyeLocation.toVector(),
            player.location.direction.normalize(),
            config.interactionRadius ?: shopPresenter.data.interactionRadius ?: 3.0
        )
        return result != null
    }

    fun spawn(tracker: NpcEntityTracker) {
        tracker.addEntity(entity)
//        NameTagManager.addNpc(entity.uuid.toString())
    }

    fun despawn(tracker: NpcEntityTracker) {
        tracker.removeEntity(entity)
//        NameTagManager.removeNpc(entity.uuid.toString())
    }
}