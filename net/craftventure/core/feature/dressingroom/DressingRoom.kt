package net.craftventure.core.feature.dressingroom

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.manager.TrackerAreaManager
import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.kart.KartManager
import net.craftventure.core.inventory.impl.BarberMainMenu
import net.craftventure.core.inventory.impl.DressingRoomPreviewPickerMenu
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.manager.Allow
import net.craftventure.core.manager.Deny
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.PlayerStateManager.isAllowedToJoinDressingRoom
import net.craftventure.core.manager.PlayerStateManager.withGameState
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.EntityUtils.nmsHandle
import net.craftventure.database.bukkit.listener.ShopCacheListener
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DressingRoom(
    val id: String,
    val directory: File,
    val data: DressingRoomDto
) : Listener, TrackerAreaManager.AreaListener {
    private var started = false
    private val shop get() = ShopCacheListener.cached(data.shop)

    private val players = ConcurrentHashMap.newKeySet<DressingRoomPlayerState>()
    private val npcs = data.npcs.map {
        val npc = NpcEntity(location = it.location, entityType = it.entityType)
        it.equipment?.equip(npc)
        it.metadata.forEach { it.applyTo(npc) }
        NpcData(it, npc)
    }
    private val npcTracker = data.npcVisibilityArea.takeIf { it.isNotEmpty() }?.let {
        val combinedArea = CombinedArea(*it.map { it.create() }.toTypedArray())
        val area = NpcAreaTracker(combinedArea)
        npcs.forEach { npc -> area.addEntity(npc.entity) }
        area
    }

    val selfNpcMountData = data.playerPreviewMountId?.let { mountId -> npcs.firstOrNull { it.data.id == mountId } }

    private var task: BukkitRunnable? = null

    fun updateFor(player: Player) {
        remove(player, false)
    }

    private fun remove(player: Player, force: Boolean): Boolean {
//        logcat { "Remove ${player.name}? (${players.map { it.player.name }}) force=$force" }
        val playerData = players.firstOrNull { it.player.entityId == player.entityId } ?: return false
//        logcat { "PlayerData ${playerData.canRemove()} ${playerData.isDestroyed()}" }
        if (force || playerData.canRemove() || playerData.isDestroyed()) {
            player.withGameState { it.dressingRoom = null }
//            logcat { "Destroy1" }
            playerData.destroy()
//            logcat { "Destroy2" }
            players.remove(playerData)
//            logcat { "Destroy3" }
            EquipmentManager.reapply(player)
//            logcat { "Destroy4" }
//            logcat { "Removed ${player.name}" }
            return true
        } else {
//            logcat { "Request destroy" }
            playerData.requestDestroy()
        }
        return false
    }

    private fun add(player: Player) {
        if (players.none { it.player === player }) {
            player.withGameState { it.dressingRoom = this }
            players.add(DressingRoomPlayerState(this, player))
            EquipmentManager.reapply(player)
            KartManager.kartForPlayer(player)?.requestDestroy()
//            logcat { "Added ${player.name}" }
        }
    }

    @EventHandler
    fun onWornItems(event: PlayerEquippedItemsUpdateEvent) {
        val player = event.player
        val data = players.find { it.player === player } ?: return

        event.appliedEquippedItems.clearAll()
    }

    @EventHandler
    fun onSpectatorTargetChanged(event: PlayerStopSpectatingEntityEvent) {
        val reference = event.spectatorTarget.getMetadata<DressingRoomPlayerState.DressingRoomReference>() ?: return
        if (reference.state.isDestroyed()) return

        if (reference.state.player === event.player) {
            if (!remove(event.player, false)) {
                reference.state.handleSneakClose()
                event.isCancelled = true
                event.player.nmsHandle.camera = event.spectatorTarget.nmsHandle
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        remove(event.player, true)
    }

    @EventHandler
    fun onPlayerAnimation(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) return
        val player = event.player
        val state = players.find { it.player === player } ?: return

        logcat { "onPlayerAnimation" }
        event.isCancelled = true

        if (state.state == DressingRoomPlayerState.State.Preview)
            openPreviewInventory(player, state)
    }

//    @EventHandler
//    fun onPlayerMove(event: PlayerLocationChangedEvent) {
//        logcat { "Player move changed=${event.lookChanged}" }
//        if (event.lookChanged) {
//            val state = players.find { it.player === event.player } ?: return
//
//            val yawDiff = AngleUtils.distance(event.from.yaw.toDouble(), event.to.yaw.toDouble())
//            val pitchDiff = AngleUtils.distance(event.from.pitch.toDouble(), event.to.pitch.toDouble())
//
//            if (yawDiff != 0.0)
//                state.rotateSelfYawByDegrees(yawDiff)
//            if (pitchDiff != 0.0)
//                state.rotateSelfPitchByDegrees(yawDiff)
//        }
//    }

//    @EventHandler
//    fun onUseEntity(event: PacketUseEntityEvent) {
//        if (!event.isAttacking) return
//        val player = event.player
//        val state = players.find { it.player === player } ?: return
//
////        logcat { "useEntity" }
//        event.isCancelled = true
//
//        if (state.state == DressingRoomPlayerState.State.Preview)
//            openPreviewInventory(player, state)
//    }

    private fun openPreviewInventory(player: Player, state: DressingRoomPlayerState) {
//        logcat { "Open preview menu ${miniTrace(10)}" }
        if (data.type == DressingRoomDto.Type.Barber) {
            val items = shop?.cachedOffers?.mapNotNull { it.ownableItem.id }?.toSet()
            val menu = BarberMainMenu(
                player,
                (items ?: emptySet()) + this.data.items,
                state = state,
            )
            menu.openAsMenu(player)
        } else {
            val items = shop?.cachedOffers?.mapNotNull { it.ownableItem.id }?.toSet()
            val menu = DressingRoomPreviewPickerMenu(
                player,
                (items ?: emptySet()) + this.data.items,
                allowedTypes = this.data.previewSlots,
                state = state,
            )
            menu.openAsMenu(player)
        }
    }

    override val area: Area
        get() = data.triggerAreaCombined

    override fun update(player: Player, location: Location, cancellable: Cancellable?) {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.DRESSING_ROOM)) return
        val inArea = location in data.triggerAreaCombined
        val wasInArea = players.any { it.player === player }

        if (inArea != wasInArea) {
            if (wasInArea) {
//                remove(player, true)
            } else if (player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR) {
                val allowResult = player.isAllowedToJoinDressingRoom()
                if (allowResult is Allow) {
                    add(player)
                } else if (allowResult is Deny) {
                    player.sendMessage(allowResult.errorComponent)
                    cancellable?.isCancelled = true
                }
            }
        }
    }

    override fun handleLogout(player: Player) {
        remove(player, true)
    }

    fun start() {
        if (started) return
//        Logger.debug("Starting shop $name")
        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance())
        TrackerAreaManager.registerTracker(this)
        npcTracker?.startTracking()
        started = true
    }

    fun stop() {
        if (!started) return
        task?.cancel()
        task = null
        players.forEach { remove(it.player, true) }
//        Logger.debug("Shopping shop $name")
        npcTracker?.stopTracking()
        TrackerAreaManager.unregisterTracker(this)
        HandlerList.unregisterAll(this)
        started = false
    }

    data class NpcData(
        val data: DressingRoomDto.NpcData,
        val entity: NpcEntity,
    )

}