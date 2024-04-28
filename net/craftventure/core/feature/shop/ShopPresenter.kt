package net.craftventure.core.feature.shop

import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.extension.isDisconnected
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.manager.TrackerAreaManager
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi
import net.craftventure.core.async.executeSync
import net.craftventure.core.feature.balloon.BalloonManager
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.holders.NpcEntityHolder
import net.craftventure.core.feature.balloon.holders.StaticLocationHolder
import net.craftventure.core.feature.shop.dto.ShopPresenterDto
import net.craftventure.core.inventory.impl.BuyItemMenu
import net.craftventure.core.inventory.impl.ShopOfferMenu
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.actor.ActorPlayback
import net.craftventure.core.npc.actor.RecordingData
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.serverevent.PacketUseEntityEvent
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.listener.ShopCacheListener
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ShopPresenter(
    val id: String,
    val directory: File,
    val data: ShopPresenterDto
) : Listener, BackgroundService.Animatable, TrackerAreaManager.AreaListener {
    private var started = false
    private val items = data.items.map { ShopItem(this, it) }
    private val entityTracker = NpcAreaTracker(data.visibilityAreaCombined)
    private val npcs = data.npcKeepers.map { npcConfig ->
        val npcFile = File(directory, "npc/${npcConfig.file}")
        if (!npcFile.exists()) throw IOException("Missing NPC file ${npcConfig.file} (${npcFile.path}) for shop $id")

        val recordingData =
            CvApi.gsonActor.fromJson(npcFile.readText(), RecordingData::class.java)
                ?: throw IOException("Failed to parse NPC ${npcConfig.file} (${npcFile.path}) for shop $id")

        var location =
            recordingData.getFirstLocation(data.visibilityAreaCombined.world)
        if (location == null) {
            Logger.severe("WARNING: failed to retrieve location from NPC! Using fallback location")
            location = data.visibilityAreaCombined.world.spawnLocation.clone()
        }

        val npcEntity = NpcEntity(
            id,
            recordingData.getPreferredType()!!,
            location,
            if (recordingData.getGameProfile() != null)
                MainRepositoryProvider.cachedGameProfileRepository.findCached(recordingData.getGameProfile())
            else null,
        )
        if (recordingData.getEquipment() != null)
            recordingData.getEquipment().equip(npcEntity)
        ShopNpcKeeper(
            this,
            ActorPlayback(npcEntity, recordingData).also {
                it.repeat = true
            },
            config = npcConfig
        )
    }
    private val staticNpcs = data.staticKeepers.map { npcConfig ->
        StaticShopNpcKeeper(this, entityTracker, npcConfig)
    }
    private val balloonHolderNpcs = data.balloonHolders.map { config ->
        val npc = NpcEntity(entityType = config.entityType, location = config.location)
        npc.marker(true)
        npc.invisible(true)
        entityTracker.addEntity(npc)
        ShopBalloonHolder(
            this,
            npc,
            config
        )
    }

    private val balloons = data.balloons.mapNotNull { config ->
        val leashHolderNpc: NpcEntity? = config.leashNpcId?.let { leashId ->
            npcs.find { it.config.id == leashId }?.entity ?: staticNpcs.find { it.data.id == leashId }?.entity
            ?: balloonHolderNpcs.find { it.config.id == leashId }?.npc
        }
        val configLocation = config.location
        if (configLocation == null && leashHolderNpc == null) {
            logcat(
                priority = LogPriority.WARN,
                logToCrew = true
            ) { "Invalid location/npc combination for balloon at shop ${this.id}, at least one of the two must be set" }
            return@mapNotNull null
        }

//        logcat { "Leash id ${leashHolderNpc?.id}" }
        val item = MainRepositoryProvider.ownableItemRepository.findCached(config.balloonId) ?: run {
            logcat(
                priority = LogPriority.WARN,
                logToCrew = true
            ) { "Invalid item (${config.balloonId}) for balloon at shop ${this.id}" }
            return@mapNotNull null
        }
        val balloon = BalloonManager.toBalloon(item) ?: run {
            logcat(
                priority = LogPriority.WARN,
                logToCrew = true
            ) { "Invalid balloon data (${config.balloonId}) for balloon at shop ${this.id}" }
            return@mapNotNull null
        }
        ShopBalloonDisplay(
            shopPresenter = this,
            holderCreator = {
                if (configLocation != null)
                    StaticLocationHolder(
                        leashHolderNpc?.entityId,
                        BalloonHolder.TrackerInfo(
                            entityTracker,
                            false,
                        ),
                        configLocation,
                        maxLeashLength = config.maxLeashLength
                    )
                else {
                    NpcEntityHolder(
                        BalloonHolder.TrackerInfo(
                            entityTracker,
                            false,
                        ),
                        leashHolderNpc!!
                    )
                }
            },
            balloon = balloon,
            config = config,
        )
    }

    private val shop get() = ShopCacheListener.cached(data.shop)

    //    private var updateTask: Int? = null
    private val players = ConcurrentHashMap.newKeySet<Player>()

    val playersInShop: Set<Player> = players

    override fun onAnimationUpdate() {
        players.removeAll { it.isDisconnected() }
//        items.forEach { it.update() }
        players.forEach { updateTargetedItem(it) }
        staticNpcs.forEach { it.update() }
    }

    private fun updateTargetedItem(player: Player) {
        val match = items.map { it to it.data.location.distance(player.location) }
            .filter { it.second <= (it.first.data.interactionRadius ?: it.first.shop.data.interactionRadius ?: 3.0) }
            .sortedBy { it.second }
            .firstOrNull { it.first.matches(player) }
            ?.first
        items.forEach {
            if (it != match) {
                val removed = it.remove(player)
                if (removed && match == null)
                    MessageBarManager.remove(player, ChatUtils.ID_SHOP_MESSAGES)
            }
        }
        if (match != null) {
            val result = match.add(player)
            if (result) {
                display(
                    player,
                    Message(
                        id = ChatUtils.ID_SHOP_MESSAGES,
                        text = Component.text(
                            "Leftclick to buy ${match.ownableItem?.type?.displayName ?: ""} ${match.item?.overridenTitle}",
                            CVTextColor.serverNotice
                        ),
                        type = MessageBarManager.Type.SHOP_PRESS_F_BUY_MESSAGE,
                        untilMillis = Long.MAX_VALUE,
                    ),
                    replace = true,
                )
            }
        } else {
            val npcMatch = npcs.firstOrNull { it.matches(player) }
            npcs.forEach { npc ->
                if (npc != npcMatch) {
                    val removed = npc.remove(player)
                    if (removed && npcMatch == null) {
                        MessageBarManager.remove(player, ChatUtils.ID_SHOP_MESSAGES)
                    }
                }
            }
            if (npcMatch != null) {
                val result = npcMatch.add(player)
                if (result) {
                    display(
                        player,
                        Message(
                            id = ChatUtils.ID_SHOP_MESSAGES,
                            text = Component.text(
                                "Click to open shop",
                                CVTextColor.serverNotice
                            ),
                            type = MessageBarManager.Type.SHOP_PRESS_F_BUY_MESSAGE,
                            untilMillis = Long.MAX_VALUE,
                        ),
                        replace = true,
                    )
                }
            } else {
                val staticNpcMatch = staticNpcs.firstOrNull { it.matches(player) }
                staticNpcs.forEach { npc ->
                    if (npc != staticNpcMatch) {
                        val removed = npc.remove(player)
                        if (removed && staticNpcMatch == null) {
                            MessageBarManager.remove(player, ChatUtils.ID_SHOP_MESSAGES)
                        }
                    }
                }
                if (staticNpcMatch != null) {
                    val result = staticNpcMatch.add(player)
                    if (result) {
                        display(
                            player,
                            Message(
                                id = ChatUtils.ID_SHOP_MESSAGES,
                                text = Component.text(
                                    "Leftclick to open shop",
                                    CVTextColor.serverNotice
                                ),
                                type = MessageBarManager.Type.SHOP_PRESS_F_BUY_MESSAGE,
                                untilMillis = Long.MAX_VALUE,
                            ),
                            replace = true,
                        )
                    }
                }
            }
        }
    }

    private fun remove(player: Player) {
        players.remove(player)
        items.forEach { it.remove(player) }
        npcs.forEach { it.remove(player) }
        MessageBarManager.remove(player, id = ChatUtils.ID_SHOP_MESSAGES)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        remove(event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPacketUseEntityEvent(event: PacketUseEntityEvent) {
        if (!event.isAttacking) return
        if (event.player !in players) return
//        Logger.debug("Shop ${shop != null}")
//        Logger.debug("Use entity")
        val match = items.firstOrNull { it.matches(event.player) }
        if (match != null) {
            event.isCancelled = true
            executeSync {
                BuyItemMenu(event.player, match.data.id).openAsMenu(event.player)
            }
        } else {
            val shop = shop ?: return
            val npcMatch = npcs.firstOrNull { it.entityId == event.interactedEntityId }
                ?: staticNpcs.firstOrNull { it.entityId == event.interactedEntityId }
            if (npcMatch != null) {
                event.isCancelled = true
                executeSync {
                    ShopOfferMenu(event.player, shop).openAsMenu(event.player)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.player !in players) return
//        Logger.debug("Shop ${shop != null}")
        val match = items.firstOrNull { it.matches(event.player) }
        if (match != null) {
            event.isCancelled = true
//            executeSync {
            BuyItemMenu(event.player, match.data.id).openAsMenu(event.player)
//            }
        } else {
            val shop = shop ?: return
            val npcMatch = npcs.firstOrNull { it.matches(event.player) }
            if (npcMatch != null) {
                event.isCancelled = true
                ShopOfferMenu(event.player, shop).openAsMenu(event.player)
            }
        }
    }

//    @EventHandler
//    fun onHotkeyPressed(event: PlayerHotKeyPressedEvent) {
//        if (event.key == PlayerHotKeyPressedEvent.Key.F) {
//            val match = items.firstOrNull { it.matches(event.player) }
//            if (match != null) {
//                event.isCancelled = true
//                BuyItemMenu(event.player, match.data.id).open(event.player)
//            }
//        } else {
//            val shop = shop ?: return
//            val npcMatch = npcs.firstOrNull { it.matches(event.player) }
//            if (npcMatch != null) {
//                ShopOfferMenu(event.player, shop)
//            }
//        }
//    }

    override val area: Area
        get() = data.visibilityAreaCombined

    override fun update(player: Player, location: Location, cancellable: Cancellable?) {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.SHOPS_PRESENTER)) return
        val inArea = location in data.visibilityAreaCombined
        val wasInArea = player in players

        if (inArea != wasInArea) {
            if (wasInArea) {
                if (players.remove(player)) {
                    items.forEach { it.remove(player) }
                    npcs.forEach { it.remove(player) }
                    staticNpcs.forEach { it.remove(player) }
                }
            } else {
                players.add(player)
            }
        }
    }

    override fun handleLogout(player: Player) {
        remove(player)
    }

    fun start() {
        if (started) return
//        Logger.debug("Starting shop $name")
        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance())
        BackgroundService.add(this)
//        updateTask = executeSync(5L, 5L, this::update)
        items.forEach { it.spawn(entityTracker) }
        staticNpcs.forEach { it.spawn(entityTracker) }
        npcs.forEach { it.spawn(entityTracker) }
        npcs.forEach { it.play() }
        balloons.forEach { it.create() }
        entityTracker.startTracking()
        TrackerAreaManager.registerTracker(this)
        started = true
    }

    fun stop() {
        if (!started) return
//        Logger.debug("Shopping shop $name")
        TrackerAreaManager.unregisterTracker(this)
        HandlerList.unregisterAll(this)
        BackgroundService.remove(this)
//        updateTask?.let { Bukkit.getScheduler().cancelTask(it) }
        items.forEach { it.despawn(entityTracker) }
        staticNpcs.forEach { it.despawn(entityTracker) }
        npcs.forEach { it.despawn(entityTracker) }
        npcs.forEach { it.stop() }
        balloons.forEach { it.destroy() }
        entityTracker.stopTracking()
        started = false
    }

}