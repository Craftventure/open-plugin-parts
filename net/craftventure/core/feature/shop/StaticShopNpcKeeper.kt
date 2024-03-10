package net.craftventure.core.feature.shop

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isDisconnected
import net.craftventure.core.feature.shop.dto.StaticShopKeeperDto
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.manager.TeamsManager
import net.craftventure.core.metadata.PlayerSpecificTeamsMeta
import net.craftventure.core.npc.EntityBitFlags
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.minusFlag
import net.craftventure.withFlag
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import java.util.*


data class StaticShopNpcKeeper(
    val shopPresenter: ShopPresenter,
    val tracker: NpcEntityTracker,
    val data: StaticShopKeeperDto,
) {
    private var areaPlayers = hashSetOf<Player>()
    private val boundingBox = EntityMetadata.getBoundingBox(data.entityType)
    private val boundingBoxAtCurrentLocation
        get() = boundingBox?.let {
            val entityLocation = data.location
            BoundingBox(
                entityLocation.x + it.minX,
                entityLocation.y + it.minY,
                entityLocation.z + it.minZ,
                entityLocation.x + it.maxX,
                entityLocation.y + it.maxY,
                entityLocation.z + it.maxZ
            )
        }
    val entity = NpcEntity(
        UUID.randomUUID().toString(),
        data.entityType,
        data.location,
        data.playerProfile?.let { MainRepositoryProvider.cachedGameProfileRepository.find(it) })
    val entityId = entity.entityId

    private val listener = object : NpcEntityTracker.Listener {
        override fun onSpawnToPlayer(player: Player, entity: NpcEntity) {
            super.onSpawnToPlayer(player, entity)
            if (entity === this@StaticShopNpcKeeper.entity) {
                val meta = player.getMetadata<PlayerSpecificTeamsMeta>() ?: return
                meta.addOrUpdate(entity, TeamsManager.getTeamDataFor(NamedTextColor.GOLD))
            }
        }

        override fun onDespawnToPlayer(player: Player, entity: NpcEntity) {
            super.onDespawnToPlayer(player, entity)
            if (entity === this@StaticShopNpcKeeper.entity) {
                val meta = player.getMetadata<PlayerSpecificTeamsMeta>() ?: return
                meta.remove(entity)
            }
        }
    }

    init {
        data.equipment?.equip(entity)
        data.metadata.forEach { it.applyTo(entity) }
    }

    inner class ProximityData(
        val player: Player,
        var yaw: Float,
        var targetYaw: Float,
        var pitch: Float,
        var targetPitch: Float,
    ) {
        val isStartState get() = yaw == entity.headYaw && pitch == entity.getLocation().pitch
    }

    private val proximityPlayers = hashSetOf<ProximityData>()

    private fun isNearbyForInteraction(player: Player): Boolean =
        player.location.distanceSquared(data.location) < (data.proximityRadiusSquared ?: (3.0 * 3.0))

    fun update() {
        val existing = proximityPlayers.iterator()
        existing.forEachRemaining {
            val player = it.player
            val inProximity = isNearbyForInteraction(player)
            val shouldRemove = !inProximity && it.isStartState
            if (shouldRemove || player.isDisconnected()) {
                removeProximity(player)
                existing.remove()
            } else {
                updateForPlayer(player, inProximity)
            }
        }
        shopPresenter.playersInShop.forEach { player ->
            if (proximityPlayers.none { it.player === player } && isNearbyForInteraction(player)) {
                addProximity(player)
            }
        }
    }

    private fun removeProximity(player: Player) {
//        logcat { "Remove ${player.name}" }
        updateForPlayer(player, false)
    }

    private fun addProximity(player: Player) {
//        logcat { "Add ${player.name}" }
        proximityPlayers.add(
            ProximityData(
                player = player,
                yaw = entity.headYaw,
                targetYaw = entity.headYaw,
                pitch = entity.getLocation().pitch,
                targetPitch = entity.getLocation().pitch,
            )
        )
        updateForPlayer(player, true)
    }

    private fun updateForPlayer(player: Player, inProximity: Boolean) {
        val data = proximityPlayers.firstOrNull { it.player === player } ?: return
        if (inProximity) {
            val location = player.location
            val lookAtTarget = entity.lookAtResult(location.x, location.y + (player.eyeHeight - 1.62), location.z)
            data.targetYaw = lookAtTarget.yaw.toFloat()
            data.targetPitch = lookAtTarget.pitch.toFloat()
//            entity.lookAt(location.x, location.y, location.z, visibleTo = listOf(player))
        } else {
            data.targetYaw = entity.headYaw
            data.targetPitch = entity.getLocation().pitch
//            entity.moveVisually(visibleTo = listOf(player), headYaw = player.location.yaw - 180f)
        }

        data.yaw = AngleUtils.smallestMoveTo(
            data.yaw.toDouble(),
            data.targetYaw.toDouble(),
            this.data.horizontalRotationSpeed / 20.0
        ).toFloat()
        data.pitch = AngleUtils.smallestMoveTo(
            data.pitch.toDouble(),
            data.targetPitch.toDouble(),
            this.data.verticalRotationSpeed / 20.0
        ).toFloat()

//        logcat { "Updating ${player.name} inProximity=$inProximity yaw=${data.yaw.format(2)}" }
        entity.moveVisually(
            visibleTo = listOf(player),
            headYaw = data.yaw,
            yaw = data.yaw,
            pitch = data.pitch,
            hasChangedYawOrPitch = true,
            hasMoved = false,
            headYawChanged = true,
        )
    }

    fun remove(player: Player): Boolean {
        if (player !in areaPlayers) return false
        areaPlayers.remove(player)
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
        setGlowing(player, true)
        return true
    }

    fun matches(player: Player): Boolean {
        val boundingBox = this.boundingBoxAtCurrentLocation ?: return false
        val result = boundingBox.rayTrace(
            player.eyeLocation.toVector(),
            player.location.direction.normalize(),
            data.interactionRadius ?: shopPresenter.data.interactionRadius ?: 3.0
        )
        return result != null
    }

    fun spawn(tracker: NpcEntityTracker) {
        tracker.addListener(listener)
        tracker.addEntity(entity)
//        NameTagManager.addNpc(entity.uuid.toString())
    }

    fun despawn(tracker: NpcEntityTracker) {
        tracker.removeEntity(entity)
        tracker.removeListener(listener)
//        NameTagManager.removeNpc(entity.uuid.toString())
    }
}