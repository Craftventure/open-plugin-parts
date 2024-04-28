package net.craftventure.core.feature.minigame.mobarena

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.BaseMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.extension.unbreakable
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.feature.minigame.BaseLobby
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigamePlayer
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.concurrent.TimeUnit


class MobArena(
    internalName: String,
    name: String,
    val minigameLevel: MobArenaLevel,
    minRequiredPlayers: Int,
    lobby: BaseLobby,
    exitLocation: Location,
    preparingTicks: Int = 0,
    minKeepPlayingRequiredPlayers: Int = 1,
    baseStoppingTicks: Int = 0,
    description: String,
    representationItem: ItemStack,
    warpName: String,
) : BaseMinigame<MobArenaPlayer>(
    internalName = internalName,
    displayName = name,
    minRequiredPlayers = minRequiredPlayers,
    exitLocation = exitLocation,
    preparingTicks = preparingTicks,
    minKeepPlayingRequiredPlayers = minKeepPlayingRequiredPlayers,
    subType = Minigame.SubType.DUNGEON,
    baseStoppingTicks = baseStoppingTicks,
    description = description,
    representationItem = representationItem,
    warpName = warpName,
) {
    private val DEBUG = false
    override val maxPlayers: Int
        get() = minigameLevel.maxPlayers
    override val levelBaseTimeLimit: Long
        get() = TimeUnit.SECONDS.toMillis(minigameLevel.playTimeInSeconds.toLong())

    override fun represent(): ItemStack = MaterialConfig.dataItem(Material.ZOMBIE_HEAD, 0)

    override fun provideDuration(): Minigame.MinigameDuration = Minigame.MinigameDuration(
        Duration.ofSeconds(minigameLevel.playTimeInSeconds.toLong()),
        Minigame.DurationType.MAXIMUM
    )

    private val baseWeapon by lazy {
        val item = ItemStack(Material.IRON_SWORD, 1, 2.toShort())
        item.unbreakable()
        item.addEnchantment(Enchantment.DAMAGE_UNDEAD, 1)
        item
    }
    private var currentWaveIndex = 0
        set(value) {
            currentWave.onFinish(this)
            field = value
//            Logger.info("New event wave is $value")
            eventWaveEnterTime = System.currentTimeMillis()
            currentWave.onStart(this)
            mobsSpawnedThisRound = 0
            extraMobCount = 0
            updateBossBar()
            players.forEach {
                it.player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 10, true, false))
            }
        }
    val currentWave: Wave
        get() = minigameLevel.waves[currentWaveIndex]
    private var eventWaveEnterTime = System.currentTimeMillis()
        set(value) {
            field = value
            lastUpdateTime = System.currentTimeMillis()
        }
    private var lastUpdateTime = System.currentTimeMillis()

    private val helmet by lazy { MainRepositoryProvider.itemStackDataRepository.findCached("costume_hazmat_helmet") }
    private val chestplate by lazy {
        MainRepositoryProvider.itemStackDataRepository.findCached("costume_hazmat_chestplate")
    }
    private val leggings by lazy { MainRepositoryProvider.itemStackDataRepository.findCached("costume_hazmat_leggings") }
    private val boots by lazy { MainRepositoryProvider.itemStackDataRepository.findCached("costume_hazmat_boots") }

    private var extraMobCount = 0
    internal var mobs = mutableListOf<LivingEntity>()
    private val bossBar = Bukkit.createBossBar(
        "",
        BarColor.YELLOW,
        BarStyle.SOLID
    )
    private var success = false
    private var gameOver = false
        set(value) {
            field = value
            gameOverTime = if (value) System.currentTimeMillis() else 0L
        }
    private var gameOverTime = 0L
    var mobsSpawnedThisRound = 0
        private set

    init {
        (minigameLevel.area as SimpleArea).loadChunks(true)

        cleanupMobs()
        updateBossBar()
    }

    private fun cleanupMobs() {
        Bukkit.getWorld("world")?.entities?.forEach {
            if (minigameLevel.area.isInArea(it.location)) {
                if (it is Monster || it is Slime || it is Animals) {
                    it.remove()
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        players.firstOrNull { it.player === event.player }?.let {
            //            Logger.info("onPlayerInteractAtEntity")
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        players.firstOrNull { it.player === event.player }?.let {
            //            Logger.info("firstOrNull")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        mobs.firstOrNull { it === event.entity }?.let {
            if (event.damager is EvokerFangs) {
                event.isCancelled = true
                return
            }
        }

        players.firstOrNull { it.player === event.damager }?.let {
            if (!mobs.any { it === event.entity }) {
//                Logger.info("onEntityDamageByEntityEvent")
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        players.firstOrNull { it.player === event.player }?.let {
            //            Logger.info("onPlayerInteractEvent")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        players.firstOrNull { it.player == event.player }?.let {
            if (isRunning) {
                if (it.metadata.inLimbo)
                    event.respawnLocation = minigameLevel.limboLocation
                else
                    event.respawnLocation = currentWave.restartLocation
            } else {
                event.respawnLocation = exitLocation
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    override fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (players.any { it.player === event.entity }) {
            if (event.cause == EntityDamageEvent.DamageCause.FALL)
                event.isCancelled = true
        }
    }

    @EventHandler
    fun onTarget(event: EntityTargetEvent) {
        if (mobs.any { it === event.entity }) {
            if (!players.any { it.player === event.target }) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        players.firstOrNull { it.player === event.entity }?.let {
            it.metadata.inLimbo = true
            executeSync(20) {
                it.player.spigot().respawn()
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntitySpawn(event: CreatureSpawnEvent) {
        if (event.entity in minigameLevel.area) {
            if (event.spawnReason == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT ||
                event.spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS ||
                event.spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM ||
                event.spawnReason == CreatureSpawnEvent.SpawnReason.DEFAULT
            ) {
                if (event.entity is Vex) {
                    event.isCancelled = true
                } else {
                    extraMobCount++
                    mobs.add(event.entity)
                }
            } else {
                if (PluginProvider.isTestServer())
                    Logger.debug("Entity spawn with reason ${event.spawnReason} denied")
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onSlimeSplit(event: SlimeSplitEvent) {
        mobs.add(event.entity)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun damage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player) {

            if (entity.health - event.finalDamage <= 0) {
                players.firstOrNull { it.player === entity }?.let {
                    putInLimbo(it)
                    event.isCancelled = true
                }
            }
        }
    }

    fun putInLimbo(mobArenaPlayer: MinigamePlayer<MobArenaPlayer>) {
//        Thread.dumpStack()
        if (!mobArenaPlayer.metadata.inLimbo) {
            mobArenaPlayer.player.sendTitleWithTicks(
                stay = 20 * 3,
                titleColor = NamedTextColor.RED, title = "You ended up in the limbo",
                subtitleColor = NamedTextColor.RED, subtitle = "If your group finishes all waves, you will resurrect"
            )
        }
        if (mobArenaPlayer.metadata.inLimbo) return
        mobArenaPlayer.metadata.inLimbo = true
        mobArenaPlayer.allowNextTeleport()
        mobArenaPlayer.player.apply {
            teleport(minigameLevel.limboLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
            health = maxHealth
            for (effect in activePotionEffects)
                removePotionEffect(effect.type)
        }
    }

    override fun update(timePassed: Long) {
        super.update(timePassed)
        if (state == Minigame.State.RUNNING) {
            if (!gameOver && players.all { it.metadata.inLimbo }) {
                gameOver = true

                for (player in players)
                    player.player.sendTitleWithTicks(
                        `in` = 20, stay = 60, out = 20,
                        titleColor = NamedTextColor.DARK_RED, title = "Your team got wiped"
                    )
            }

            for (player in players) {
                player.metadata.update(level = minigameLevel)

                if (!isStartDelayed() && player.metadata.lastTeleportedWave != currentWave) {
                    if (currentWave.teleportOnStart) {
                        player.metadata.lastTeleportedWave = currentWave
                        putInLimbo(player)
//                        player.allowNextTeleport()
//                        player.player.teleport(currentWave.restartLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    }
                }
            }

            val sortedPlayers = sortedPlayers()

            for (player in sortedPlayers) {
                val timeLeft = maxGameTimeLength() - playTime
                display(
        player.player,
        Message(
            id = ChatUtils.ID_MINIGAME,
            text = Component.text(
                        "Impending meltdown in ${DateUtils.format(timeLeft, "?")}",
                        CVTextColor.serverNotice
                    ),
            type = MessageBarManager.Type.MINIGAME,
            untilMillis = TimeUtils.secondsFromNow(1.0),
        ),
        replace = true,
    )

//                if (player.player.location.block.type == Material.WATER) {
//                    if (player.lastWaterDamage < System.currentTimeMillis() - 400 && !player.player.isDead) {
//                        player.lastWaterDamage = System.currentTimeMillis()
////                        TitleUtil.send(player.player, stay = 60, `in` = 0, title = "§cOof! The water is lava!")
//                        player.player.playSound(player.player.location, Sound.ENTITY_PLAYER_HURT_DROWN, 1f, 1f)
//                        player.player.damage(2.0)
//                    }
//                }
            }

            val previousTime = lastUpdateTime - eventWaveEnterTime
            val nowTime = System.currentTimeMillis() - eventWaveEnterTime
            lastUpdateTime = System.currentTimeMillis()

            mobs.removeAll {
                if (!it.isValid) {
                    return@removeAll true
                }
                return@removeAll false
            }
//            Logger.info("previousTime=$previousTime nowTime=$nowTime currentWaveIndex=$currentWaveIndex")
            updateBossBar()
            if (!gameOver) {
                currentWave.update(this, previousTime, nowTime)
                if (mobsSpawnedThisRound < currentWave.mobCount) {
                    if (!isStartDelayed()) {
                        var spawned = true
                        var spawnCountThisTick = 0
                        while (spawned) {
                            spawned = false
                            if (mobsSpawnedThisRound < currentWave.mobCount) {
                                val spawnedMob = currentWave.spawnMob(
                                    this,
                                    mobsSpawnedThisRound,
                                    mobs.size,
                                    spawnCountThisTick,
                                    previousTime,
                                    nowTime
                                )
                                if (spawnedMob != null) {
                                    mobsSpawnedThisRound++
                                    spawnCountThisTick++
                                    (spawnedMob as? Monster)?.target = players.random().player
                                    mobs.add(spawnedMob)
                                    spawned = true
                                }
                            }
                        }
                    }
                } else if (mobsSpawnedThisRound >= currentWave.mobCount) {
                    if (mobs.size == 0) {
                        if (currentWaveIndex + 1 >= minigameLevel.waves.size) {
                            Logger.info("Finishing succesfully")
                            success = true
                            stop(Minigame.StopReason.ALL_PLAYERS_FINISHED)
                        } else {
                            currentWaveIndex++
                        }
                    }
                }
            }

            if (gameOver && System.currentTimeMillis() - gameOverTime > 5000) {
                stop(Minigame.StopReason.FAILURE)
            }
        }
    }

    fun leaveWithToken(player: MinigamePlayer<MobArenaPlayer>) {
        // TODO: Give token for current wave
        leave(player.player, Minigame.LeaveReason.LEAVE)
    }

    fun continueWave(player: MinigamePlayer<MobArenaPlayer>) {
        player.allowNextTeleport()
        player.player.teleport(currentWave.restartLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
        player.metadata.lastTeleportedWave = currentWave
    }

    fun resurrectTheDead() {
        players.forEach {
            if (it.metadata.inLimbo) {
                it.metadata.inLimbo = false
                continueWave(it)
            }
            if (it.player.isDead) {
                it.player.spigot().respawn()
            }
            it.player.health = it.player.maxHealth
        }
    }

    fun isStartDelayed(): Boolean {
        val nowTime = System.currentTimeMillis() - eventWaveEnterTime
        return nowTime - currentWave.startDelayInMillis < 0
    }

    private fun updateBossBar() {
        if (gameOver) {
            val title = "§cYou failed"
            if (bossBar.title != title) {
                bossBar.setTitle(title)
                bossBar.progress = 0.0
            }
            return
        }
        val nowTime = System.currentTimeMillis() - eventWaveEnterTime
        val isStartDelayed = isStartDelayed()
        if (isStartDelayed) {
            val secondsUntilStart = Math.abs(nowTime - currentWave.startDelayInMillis) / 1000.0
            val title =
                "§7Prepare for a battle at ${currentWave.locationName} starting in ${secondsUntilStart.format(1)} seconds"
            if (bossBar.title != title)
                bossBar.setTitle(title)
            bossBar.progress = nowTime / currentWave.startDelayInMillis.toDouble()
        } else {
            val title = "§6Defeat the mobs at ${currentWave.locationName}"
            if (bossBar.title != title)
                bossBar.setTitle(title)
            bossBar.progress =
                (((mobsSpawnedThisRound + extraMobCount) - mobs.sumOf { it.health / it.maxHealth }) / (currentWave.mobCount + extraMobCount).toDouble()).clamp(
                    0.0,
                    1.0
                )
        }
    }

    private fun reset() {
        success = false
        gameOver = false
        gameOverTime = 0L
        bossBar.isVisible = true
        bossBar.progress = 0.0
        extraMobCount = 0

        mobsSpawnedThisRound = 0
        currentWaveIndex = 0

        for (wave in minigameLevel.waves) {
            wave.onReset(this)
        }
        updateBossBar()
    }

    override fun onStateChanged(oldState: Minigame.State, newState: Minigame.State) {
        super.onStateChanged(oldState, newState)

        if (newState == Minigame.State.RUNNING) {
            reset()
            currentWaveIndex = 0
        }
    }

    override fun onUpdatePlayerWornItems(
        player: MinigamePlayer<MobArenaPlayer>,
        event: PlayerEquippedItemsUpdateEvent
    ) {
        super.onUpdatePlayerWornItems(player, event)

        event.appliedEquippedItems.clearArmor()
        event.appliedEquippedItems.clearSpecials()

        if (state == Minigame.State.PREPARING_GAME || state == Minigame.State.RUNNING) {
            event.appliedEquippedItems.balloonItem = null
//        event.wornData.miscItem = null
            event.appliedEquippedItems.title = null

            event.appliedEquippedItems.weaponItem = baseWeapon.toEquippedItemData()
            event.appliedEquippedItems.helmetItem = helmet?.itemStack?.toEquippedItemData()
            event.appliedEquippedItems.chestplateItem = chestplate?.itemStack?.toEquippedItemData()
            event.appliedEquippedItems.leggingsItem = leggings?.itemStack?.toEquippedItemData()
            event.appliedEquippedItems.bootsItem = boots?.itemStack?.toEquippedItemData()
            player.player.inventory.heldItemSlot = 4
        }
    }

    override fun onPreJoin(player: Player) {
        super.onPreJoin(player)
        player.teleport(currentWave.restartLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
        player.gameMode = GameMode.ADVENTURE
        bossBar.addPlayer(player)
    }

    override fun provideMeta(player: Player): MobArenaPlayer = MobArenaPlayer().apply {
        lastTeleportedWave = currentWave
    }

    override fun onPlayerJoined(player: MinigamePlayer<MobArenaPlayer>) {
        super.onPlayerJoined(player)
        player.allowNextTeleport()
    }

    override fun onPlayerLeft(minigamePlayer: MinigamePlayer<MobArenaPlayer>, reason: Minigame.LeaveReason) {
        super.onPlayerLeft(minigamePlayer, reason)
        bossBar.removePlayer(minigamePlayer.player)
    }

    override fun onPreStop(stopReason: Minigame.StopReason) {
        mobs.forEach { it.remove() }

        bossBar.removeAll()

//        Logger.info("Mobarena stopping $stopReason with ${players.size} players gameOver=$gameOver success=$success")
        if (stopReason == Minigame.StopReason.FAILURE) {
            players.forEach {
                if (it.player.isDead) {
                    it.player.spigot().respawn()
                }
                it.player.sendTitleWithTicks(
                    `in` = 20, stay = 60, out = 20,
                    titleColor = NamedTextColor.DARK_RED, title = "You failed miserably",
                    subtitleColor = NamedTextColor.RED, subtitle = "Better come prepared next time..."
                )
            }
            return
        }

        val finishedNormally =
            !gameOver && success && (stopReason == Minigame.StopReason.ALL_PLAYERS_FINISHED || stopReason == Minigame.StopReason.OUT_OF_TIME)
        when {
            finishedNormally -> {
                val players = sortedPlayers()
                for (minigamePlayer in players) {
                    val player = minigamePlayer.player
//                    player.sendMessage(CVChatColor.COMMAND_GENERAL + "You helped to fix Gon Bao's little mistake! Thank you for helping!")

                    player.getOrCreateMetadata { MobArenaWheelData() }.finishTickets++
                    executeAsync {
                        //                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(player.uniqueId, "legendary_gladiator", -1)
                        //                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(player.uniqueId, "candy_cane", -1)
//                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(player.uniqueId, "gonbao_lever_hat", -1)
                        MainRepositoryProvider.achievementProgressRepository
                            .increaseCounter(player.uniqueId, "minigame_${internalName}_win")
                    }
                }
            }

            stopReason == Minigame.StopReason.OUT_OF_TIME -> players
                .asSequence()
                .map { it.player }
                .forEach { it.sendMessage(CVTextColor.serverNotice + "You failed") }

            else -> players
                .asSequence()
                .map { it.player }
                .forEach { Translation.MINIGAME_TOO_FEW_PLAYERS.getTranslation(it, displayName)?.sendTo(it) }
        }
    }

    override fun toJson(): Json = toJson(Json())

    abstract class Wave(
        val startDelayInMillis: Int,
        val restartLocation: Location,
        val mobSpawnLocations: Array<Location>,
        val mobCount: Int,
        val locationName: String,
        val teleportOnStart: Boolean = false
    ) {
        abstract fun spawnMob(
            mobArena: MobArena,
            mobId: Int,
            activeMobCount: Int,
            mobsSpawnedThisTick: Int,
            previousTime: Long,
            nowTime: Long
        ): LivingEntity?

        open fun update(mobArena: MobArena, previousTime: Long, nowTime: Long) {}
        open fun onStart(mobArena: MobArena) {}
        open fun onFinish(mobArena: MobArena) {}
        open fun onReset(mobArena: MobArena) {}
    }

    class MobArenaWheelData : BaseMetadata() {
        var finishTickets = 0

        //            set(value) {
//                if (field != value) {
//                    field = value
//                }
//            }
        override fun debugComponent() = Component.text("finishTickets=$finishTickets")
    }

    class Json : BaseMinigame.Json() {
        override fun createGame(): Minigame {
            TODO("Not yet implemented")
        }
    }
}