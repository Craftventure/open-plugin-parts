package net.craftventure.core.feature.dressingroom

import com.destroystokyo.paper.ClientOption
import net.craftventure.bukkit.ktx.entitymeta.BaseEntityMetadata
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.bukkit.ktx.manager.BossBarManager
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.animation.interpolation.InterpolationUtils
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.spawn
import net.craftventure.core.feature.balloon.BalloonManager
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.holders.StaticLocationHolder
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.manager.TeamsManager
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.metadata.PlayerSpecificTeamsMeta
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.ManualNpcTracker
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.EntityUtils.nmsHandle
import net.craftventure.database.type.EquippedItemSlot
import net.craftventure.database.type.ItemType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration

class DressingRoomPlayerState(
    val room: DressingRoom,
    val player: Player,
) {
    private var playerEquippedItems: Map<EquippedItemSlot, EquipmentManager.EquippedItemData>? = emptyMap()
    private var equippedItems: Map<ItemType, ItemStack?> = emptyMap()
    private val previewItems = mutableMapOf<EquippedItemSlot, EquipmentManager.EquippedItemData>()
    private val titleFadeTimeInMs = 750L
    private val fadeVisibilityTime = 150L
    private val fadeVisibilityTimeHalf = (fadeVisibilityTime * 0.5).toLong()
    private var lastStateChange: Long = System.currentTimeMillis()
    var state: State = State.Loading
        private set(value) {
            if (field != value) {
                logcat { "State changed for ${player.name}" }
                isFading = false
                val oldValue = field
                field = value
                lastStateChange = System.currentTimeMillis()
                leaveStateAt = null
                onNewState(value, oldValue)
            }
        }
    private var isFading = false
    private var destroyed: Boolean = false
    private var canDestroy: Boolean = false
    private val entityTracker: ManualNpcTracker = ManualNpcTracker(player)

    // Mapped by slot
    private val wornItemNpcs = hashMapOf<String, NpcEntity>()

    private val initialLocation: Location = player.location.clone()
    private val currentCameraLocation: Location = player.location.clone()
//    private var targetLocation: Location = room.data.cameraLocations.default.location
//    private var currentZoom: Int = 0

    private var destroyRequested: Boolean = false
    private var leaveStateAt: Long? = null

    private fun updateCachedEquippedItems() {
        playerEquippedItems = player.equippedItemsMeta()?.equippedItems
        equippedItems = mapOf(
            ItemType.HELMET to (playerEquippedItems?.get(EquippedItemSlot.HELMET)?.itemStack
                ?: playerEquippedItems?.get(
                    EquippedItemSlot.HAIRSTYLE
                )?.itemStack),
            ItemType.CHESTPLATE to playerEquippedItems?.get(EquippedItemSlot.CHESTPLATE)?.itemStack,
            ItemType.LEGGINGS to playerEquippedItems?.get(EquippedItemSlot.LEGGINGS)?.itemStack,
            ItemType.BOOTS to playerEquippedItems?.get(EquippedItemSlot.BOOTS)?.itemStack,
            ItemType.WEAPON to playerEquippedItems?.get(EquippedItemSlot.HANDHELD)?.itemStack,
            ItemType.WEAPON to playerEquippedItems?.get(EquippedItemSlot.HANDHELD)?.itemStack,
        )
    }

    private var camera: ArmorStand? = null
    private var shouldRespawnCamera = true
    private fun requireCamera(location: Location = currentCameraLocation, forceRespawn: Boolean = shouldRespawnCamera) =
        camera?.takeIf { it.isValid && !forceRespawn } ?: location.spawn<ArmorStand>().apply {
            if (forceRespawn) {
                logcat { "Respawning camera" }
            }
            val oldCamera = camera
            val isTarget = player.nmsHandle.camera.id == oldCamera?.entityId
            shouldRespawnCamera = false
            executeSync { oldCamera?.remove() }
            isInvisible = true
            camera = this
            isInvulnerable = true
            if (isTarget) {
                player.nmsHandle.camera = this.nmsHandle
            }
            getOrCreateMetadata { DressingRoomReference(this@DressingRoomPlayerState) }
        }

    private val selfNpc = NpcEntity("dressingroom/${player.name}", player, currentCameraLocation)

    private val introAnimation = SimpleAnimation(room.data.intro, initialLocation)
    private val outroAnimation = SimpleAnimation(room.data.outro, initialLocation)

    private val task: BukkitRunnable

    private var balloonHolder: BalloonHolder? = null

    init {
        updateCachedEquippedItems()
        if (room.data.type == DressingRoomDto.Type.Barber) {
            playerEquippedItems?.get(EquippedItemSlot.HAIRSTYLE)?.let {
                previewItems[EquippedItemSlot.HAIRSTYLE] = it
            }
        }
        selfNpc.setMetadata(
            EntityMetadata.Player.customization,
            player.getClientOption(ClientOption.SKIN_PARTS).raw.toByte()
        )
        equipSelfNpcWithWornData()
        entityTracker.addEntity(selfNpc)

        player.allowFlight = true
        player.isFlying = true
        player.isGliding = false

        player.getMetadata<PlayerSpecificTeamsMeta>()?.addOrUpdate(
            selfNpc, TeamsManager.getTeamDataFor(
                NamedTextColor.GOLD
            )
        )

        player.displayTitle(
            TitleManager.TitleData.ofFade(
                id = "dressingroom",
                type = TitleManager.Type.Fade,
                times = Title.Times.times(
                    Duration.ofMillis(titleFadeTimeInMs),
                    Duration.ofMillis(fadeVisibilityTime),
                    Duration.ofMillis(titleFadeTimeInMs)
                ),
            ),
            replace = true,
        )

        task = object : BukkitRunnable() {
            override fun run() {
                tick()
            }
        }
        task.runTaskTimer(PluginProvider.plugin, 1, 1)
    }

    private fun removeHolder() {
        balloonHolder?.let { BalloonManager.remove(it) }
        balloonHolder = null
    }

    private fun recreateHolder(): BalloonHolder {
        removeHolder()
        balloonHolder = StaticLocationHolder(
            selfNpc.entityId,
            BalloonHolder.TrackerInfo(
                entityTracker,
                false
            ),
            room.data.previewLocation.clone(),
            maxLeashLength = 2.3,
        )
        return balloonHolder!!
    }

    fun getEquipment(equippedItemSlot: EquippedItemSlot) = previewItems[equippedItemSlot]
    fun getActualEquipment(equippedItemSlot: EquippedItemSlot) = playerEquippedItems?.get(equippedItemSlot)

    fun getAllEquipment(): Map<EquippedItemSlot, EquipmentManager.EquippedItemData> = previewItems

    fun rotateSelfYawByDegrees(degrees: Double) {
        val yaw = (selfNpc.getLocation().yaw + degrees.toFloat()) % 360
        selfNpc.move(
            yaw = yaw,
            headYaw = yaw,
        )
//        selfNpc.animate(AnimateType.SWING_MAIN_HAND)
    }

    fun rotateSelfPitchByDegrees(degrees: Double) {
        val yaw = (selfNpc.getLocation().yaw + degrees.toFloat()) % 360
        selfNpc.move(
            yaw = yaw,
            headYaw = yaw,
        )
//        selfNpc.animate(AnimateType.SWING_MAIN_HAND)
    }

    fun setEquipment(equippedItemSlot: EquippedItemSlot, item: EquipmentManager.EquippedItemData?) {
//        logcat { "Setting ${equippedItemSlot} to ${item?.id}/${item?.itemStack != null}/${item}" }
        if (item == null || (room.data.type == DressingRoomDto.Type.DressingRoom && previewItems[equippedItemSlot] == item))
            previewItems.remove(equippedItemSlot)
        else
            previewItems[equippedItemSlot] = item
//        logcat { "Updating slot $equippedItemSlot" }

        if (state == State.Preview) {
            equipSelfNpcWithPreviewData()
        }
    }

    private fun ItemStack.requireAmount(): ItemStack {
        return this.apply {
            amount = amount.clamp(1, 64)
        }
    }

    private fun equipSelfNpcWithPreviewData() {
        if (room.data.type == DressingRoomDto.Type.Barber) {
            equipSelfNpcWithWornData()
            if (state == State.Preview) {
                selfNpc.helmet(previewItems[EquippedItemSlot.HAIRSTYLE]?.itemStack?.requireAmount())
            } else {
                selfNpc.helmet(
                    previewItems[EquippedItemSlot.HAIRSTYLE]?.itemStack?.requireAmount() ?: playerEquippedItems?.get(
                        EquippedItemSlot.HELMET
                    )?.itemStack
                )
            }
        } else {
            val costume = previewItems[EquippedItemSlot.COSTUME]?.costumeItems ?: previewItems
//        logcat { "Updating preview data ${costume.entries.map { "${it.key} > ${it.value?.itemStack?.type}" }}" }
            selfNpc.helmet(costume[EquippedItemSlot.HELMET]?.itemStack?.requireAmount())
            selfNpc.chestplate(costume[EquippedItemSlot.CHESTPLATE]?.itemStack?.requireAmount())
            selfNpc.leggings(costume[EquippedItemSlot.LEGGINGS]?.itemStack?.requireAmount())
            selfNpc.boots(costume[EquippedItemSlot.BOOTS]?.itemStack?.requireAmount())
            selfNpc.held(previewItems[EquippedItemSlot.HANDHELD]?.itemStack?.requireAmount())
            selfNpc.heldOffHand(null)
        }


        val balloonData = previewItems[EquippedItemSlot.BALLOON]
        if (balloonData == null) removeHolder()
        else
            recreateHolder().let { balloonHolder ->
//            logcat { "Removing current holder" }
//            BalloonManager.remove(balloonHolder)
//                logcat { "Loading slot" }
//                logcat { "Loading balloon" }
                val balloon = BalloonManager.toBalloon(balloonData) ?: return@let
//                logcat { "Creating ${balloon::class.simpleName}" }
                BalloonManager.create(balloonHolder, balloon)
            }
    }

    private fun equipSelfNpcWithWornData() {
        equippedItems.filter {
            if (room.data.type == DressingRoomDto.Type.Barber) {
                it.key == ItemType.HELMET || it.key == ItemType.HAIRSTYLE || it.key == ItemType.CHESTPLATE || it.key == ItemType.LEGGINGS || it.key == ItemType.BOOTS
            } else true
        }.forEach { selfNpc.setSlot(it.key, it.value) }
    }

    fun tickAsync() {
        if (destroyed) return
    }

    fun tick() {
        if (destroyed) return

//        if (destroyRequested) {
//            state = State.Outro
//        }

        when (state) {
            State.Loading -> animateLoading()
            State.Intro -> animateIntro()
            State.Preview -> animatePreview()
            State.Outro -> animateOutro()
        }
    }

    private fun onNewState(state: State, oldState: State) {
        shouldRespawnCamera = true
        updateCachedEquippedItems()
        if (state == State.Preview || room.data.type == DressingRoomDto.Type.Barber) {
            equipSelfNpcWithPreviewData()
        } else {
            equipSelfNpcWithWornData()
        }

        if (state == State.Preview) {
            val text: Array<Component> = when (room.data.type) {
                DressingRoomDto.Type.DressingRoom -> arrayOf(
                    CVTextColor.serverNoticeAccent + "Left click to open the preview menu",
                    CVTextColor.serverNoticeAccent + "Sneak to leave this dressing room",
                )

                DressingRoomDto.Type.Barber -> arrayOf(
                    CVTextColor.serverNoticeAccent + "Left click to choose your hairstyle",
                    CVTextColor.serverNoticeAccent + "Sneak to leave this barbershop",
                )
            }
            BossBarManager.display(
                player,
                BossBarManager.Message(
                    id = "dressing_room",
                    text = text,
                    BossBarManager.Priority.dressingRoom,
                    Long.MAX_VALUE,
                ),
                true
            )
        } else {
            BossBarManager.remove(player, "dressing_room")
        }

        if (state == State.Preview) {
            selfNpc.move(room.data.previewLocation)
        } else if (state == State.Outro) {
            entityTracker.removeEntity(selfNpc)
            selfNpc.move(outroAnimation.playerLocationAt(0.0))
            entityTracker.addEntity(selfNpc)
        }

//        if (state == State.Preview || oldState == State.Preview) {
//            executeSync { WornItemManager.update(player) }
//        }
//        logcat { "Cleaning up $state for ${player.name}" }
        if (oldState == State.Preview) {
            wornItemNpcs.forEach { entityTracker.removeEntity(it.value) }
            wornItemNpcs.clear()
        } else if (state == State.Preview) {
            playerEquippedItems?.let { wornData ->
                equippedItems.forEach itemLoop@{ (type, item) ->
                    val displays = room.data.itemDisplays.clone()
                    displays.shuffle()
                    displays.forEach { display ->
                        if (type !in display.types) return@forEach
                        if (wornItemNpcs[display.slot] != null) return@forEach

                        val npc = NpcEntity("", entityType = EntityType.ARMOR_STAND, location = display.location)
                        npc.invisible(true)
                        npc.noGravity(true)
                        npc.setSlot(type, item)
                        wornItemNpcs[display.slot] = npc

                        return@itemLoop
                    }
                }
            }

            wornItemNpcs.forEach { entityTracker.addEntity(it.value) }
            selfNpc.move(room.data.previewLocation)
        }
    }

    private fun animateLoading() {
        val now = System.currentTimeMillis()
        val timeInState = now - lastStateChange

        if (timeInState > titleFadeTimeInMs + fadeVisibilityTimeHalf) {
            if (!isFading) {
                isFading = true
                entityTracker.startTracking()

                initialLocation.set(player.location)
                val camera = requireCamera()

                player.isInvulnerable = true
                player.flySpeed = 0f
                player.walkSpeed = 0f
                player.isInvisible = true
                EntityUtils.teleport(player, camera.location)
                player.nmsHandle.camera = camera.nmsHandle

                state = State.Intro
            }
        }
    }

    private fun animateIntro() {
        val now = System.currentTimeMillis()
        val timeInState = now - lastStateChange

        val playerLocation = introAnimation.playerLocationAt(timeInState / 1000.0)
        val cameraLocation = introAnimation.cameraLocationAt(timeInState / 1000.0)

        selfNpc.move(playerLocation)
        EntityUtils.teleport(requireCamera(cameraLocation), cameraLocation)

        if (!isFading && timeInState > room.data.intro.durationInMs - titleFadeTimeInMs - fadeVisibilityTimeHalf) {
            isFading = true

            player.displayTitle(
                TitleManager.TitleData.ofFade(
                    id = "dressingroom",
                    type = TitleManager.Type.Fade,
                    times = Title.Times.times(
                        Duration.ofMillis(titleFadeTimeInMs),
                        Duration.ofMillis(fadeVisibilityTime),
                        Duration.ofMillis(titleFadeTimeInMs)
                    ),
                ),
                replace = true,
            )
        }

        if (timeInState > room.data.intro.durationInMs) {
            state = State.Preview

            val id = room.selfNpcMountData?.entity?.entityId
            if (id != null) {
                selfNpc.mount(id)
            }
        }
    }

    private fun animatePreview() {
        val now = System.currentTimeMillis()
        val timeInState = now - lastStateChange

        val cameraLocation = room.data.cameraLocations.default.location.clone()
        val camera = requireCamera(cameraLocation)
        if (camera.location != cameraLocation) {
            EntityUtils.teleport(camera, cameraLocation)
        }

        if (leaveStateAt == null && destroyRequested && timeInState > titleFadeTimeInMs + fadeVisibilityTimeHalf + 500) {
            leaveStateAt = now + titleFadeTimeInMs + fadeVisibilityTimeHalf

            player.displayTitle(
                TitleManager.TitleData.ofFade(
                    id = "dressingroom",
                    type = TitleManager.Type.Fade,
                    times = Title.Times.times(
                        Duration.ofMillis(titleFadeTimeInMs),
                        Duration.ofMillis(fadeVisibilityTime),
                        Duration.ofMillis(titleFadeTimeInMs)
                    ),
                ),
                replace = true,
            )
        }

        if (leaveStateAt != null && now > leaveStateAt!!) {
            state = State.Outro

            selfNpc.unmount()
        }
    }

    private fun animateOutro() {
        val timeInState = System.currentTimeMillis() - lastStateChange

        val playerLocation = outroAnimation.playerLocationAt(timeInState / 1000.0)
        val cameraLocation = outroAnimation.cameraLocationAt(timeInState / 1000.0)

        selfNpc.move(playerLocation)
        EntityUtils.teleport(requireCamera(), cameraLocation)

        if (!isFading && timeInState > room.data.outro.durationInMs - titleFadeTimeInMs - fadeVisibilityTimeHalf) {
            isFading = true

            player.displayTitle(
                TitleManager.TitleData.ofFade(
                    id = "dressingroom",
                    type = TitleManager.Type.Fade,
                    times = Title.Times.times(
                        Duration.ofMillis(titleFadeTimeInMs),
                        Duration.ofMillis(fadeVisibilityTime),
                        Duration.ofMillis(titleFadeTimeInMs)
                    ),
                ),
                replace = true,
            )
        }

        if (timeInState > room.data.outro.durationInMs) {
//            logcat { "Notify destroy" }
            player.nmsHandle.camera = null
            notifyDestroy()
            room.updateFor(player)
        }
    }

    private fun notifyDestroy() {
        if (!canDestroy) {
            canDestroy = true
            executeSync { player.nmsHandle.camera = null }
        }
    }

    fun canRemove() = canDestroy

    fun isDestroyed() = destroyed

    fun handle(event: PlayerEquippedItemsUpdateEvent) {
        if (state == State.Preview) {
            event.appliedEquippedItems.clearAll()
        }
    }

    fun destroy() {
//        logcat { "Destroy" }
        if (destroyed) return
        task.cancel()
        destroyed = true
        entityTracker.release()

        BossBarManager.remove(player, "dressing_room")
        player.getMetadata<PlayerSpecificTeamsMeta>()?.remove(selfNpc)
        player.nmsHandle.camera = null

        GameModeManager.setDefaultFly(player)
        GameModeManager.setDefaultWalkSpeed(player)
        player.isInvulnerable = false
        player.isInvisible = false
        player.teleport(room.data.leaveLocation)
        camera?.remove()

        balloonHolder?.let { BalloonManager.remove(it) }
        balloonHolder = null
//        logcat { "Destroy finished" }
    }

    fun handleSneakClose() {
        if (room.data.type == DressingRoomDto.Type.Barber) {
            setEquipment(EquippedItemSlot.HAIRSTYLE, playerEquippedItems?.get(EquippedItemSlot.HAIRSTYLE))
        }
    }

    fun requestDestroy() {
        if (state != State.Loading && state != State.Intro)
            destroyRequested = true
    }

    class DressingRoomReference(
        val state: DressingRoomPlayerState,
    ) : BaseEntityMetadata() {
        override fun debugComponent() = Component.text("state=${state.state}")
    }

    enum class State {
        Loading,
        Intro,
        Preview,
        Outro,
    }

    private class SimpleAnimation(
        val source: DressingRoomDto.SimpleAnimationDto,
        val initialLocation: Location?,
    ) {
        private val player = source.player.map { TimedLocation(it.location.clone(), it.at) }.toMutableList()
        private val camera = source.camera.map { TimedLocation(it.location.clone(), it.at) }.toMutableList()

        init {
            if (initialLocation != null) {
                addInitialLocation(initialLocation)
            } else {
                player.sortBy { it.at }
                camera.sortBy { it.at }
            }
        }

        fun addInitialLocation(location: Location) {
            if (player.none { it.at == 0.0 }) {
                player.add(TimedLocation(location, 0.0))
            }
            if (camera.none { it.at == 0.0 }) {
                camera.add(TimedLocation(location, 0.0))
            }

            player.sortBy { it.at }
            camera.sortBy { it.at }
        }

        private fun locationAt(locations: List<TimedLocation>, at: Double): Location {
            val lower = locations.lastOrNull { it.at <= at }
            val upper = locations.firstOrNull { it.at > at }

            return when {
                upper != null && lower != null -> lerp(lower, upper, at)
                upper != null && lower == null -> upper.location
                upper == null && lower != null -> lower.location
                else -> locations.first().location
            }
        }

        fun playerLocationAt(at: Double): Location = locationAt(player, at)
        fun cameraLocationAt(at: Double): Location = locationAt(camera, at)

        private fun lerp(lower: TimedLocation, upper: TimedLocation, at: Double): Location {
            val t = (at - lower.at) / (upper.at - lower.at)

            val lowerLocation = lower.location
            val upperLocation = upper.location

            val yawDistance = AngleUtils.distance(lowerLocation.yaw.toDouble(), upperLocation.yaw.toDouble())
            val pitchDistance = AngleUtils.distance(lowerLocation.pitch.toDouble(), upperLocation.pitch.toDouble())

//            logcat {
//                "Lerping ${t.format(2)} " +
//                        "yaw=${yawDistance.format(2)} " +
//                        "pitch=${pitchDistance.format(2)} " +
//                        "lower=${lower.at.format(2)} " +
//                        "upper=${upper.at.format(2)} "
//            }

            return Location(
                lowerLocation.world,
                InterpolationUtils.linear(lowerLocation.x, upperLocation.x, t),
                InterpolationUtils.linear(lowerLocation.y, upperLocation.y, t),
                InterpolationUtils.linear(lowerLocation.z, upperLocation.z, t),
                lowerLocation.yaw + (yawDistance * t).toFloat(),
                lowerLocation.pitch + (pitchDistance * t).toFloat(),
            )
        }

        class TimedLocation(
            val location: Location,
            val at: Double,
        )
    }
}