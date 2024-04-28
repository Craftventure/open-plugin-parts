package net.craftventure.core.feature.minigame.autopia

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.spawn
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.kart.KartManager
import net.craftventure.core.feature.kart.KartOwner
import net.craftventure.core.feature.kart.KartRespawnData
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigamePlayer
import net.craftventure.core.ktx.extension.asOrdinalAppended
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.serverevent.PlayerStuckEvent
import net.craftventure.core.utils.ItemStackUtils
import net.craftventure.core.utils.LookAtUtil
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

class Autopia(
    id: String,
    val minigameLevel: AutopiaLevel,
    minRequiredPlayers: Int = 2,
    minKeepPlayingRequiredPlayers: Int = 2,
    private var kartId: String = "autopia",
    name: String,
    exitLocation: Location,
    private var applyColors: Boolean = true,
    description: String,
    representationItem: ItemStack,
    warpName: String?,
    private val rewardWin: Int = 10,
    private val rewardPlay: Int = 5,
    private val balanceType: BankAccountType = BankAccountType.VC,
    private val colors: Array<KartColor> = arrayOf(
        KartColor(colorFromHex("#2c2e8f")),
        KartColor(colorFromHex("#603b1f")),
        KartColor(colorFromHex("#f0ae15")),
        KartColor(colorFromHex("#36393d")),
        KartColor(colorFromHex("#4a5c25")),
        KartColor(colorFromHex("#2389c7")),
        KartColor(colorFromHex("#60aa1b")),
        KartColor(colorFromHex("#e06100")),
        KartColor(colorFromHex("#d5648e")),
        KartColor(colorFromHex("#631f9b")),
        KartColor(colorFromHex("#8f2121")),
        KartColor(colorFromHex("#000000"))
    ),
    saveScores: Boolean,
    private val winDisplays: List<WinDisplay>,
) : BaseMinigame<AutopiaPlayer>(
    internalName = id,
    displayName = name,
    minRequiredPlayers = minRequiredPlayers,
    exitLocation = exitLocation,
    minKeepPlayingRequiredPlayers = minKeepPlayingRequiredPlayers,
    description = description,
    representationItem = representationItem,
    warpName = warpName,
    saveScores = saveScores,
), KartOwner {
    private var updateDistanceTick = 0
    private var playedCountdownSound = false
    var debugAngle = 0.0
    override val maxPlayers: Int
        get() = minigameLevel.maxPlayers
    override val levelBaseTimeLimit: Long
        get() = TimeUnit.SECONDS.toMillis(minigameLevel.playTimeInSeconds.toLong())

    override fun allowUserDestroying(): Boolean = false

    override fun provideDuration(): Minigame.MinigameDuration = Minigame.MinigameDuration(
        Duration.ofSeconds(minigameLevel.playTimeInSeconds.toLong()),
        Minigame.DurationType.MAXIMUM
    )

    override fun isKartEnabled(kart: Kart): Boolean {
        if (state == Minigame.State.RUNNING) {
            for (player in players) {
                if (player.metadata.kart === kart) {
                    return player.metadata.lap <= minigameLevel.laps
                }
            }
        }
        return false
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerStuck(event: PlayerStuckEvent) {
        if (state == Minigame.State.RUNNING) {
            for (player in players) {
                if (event.player === player.player) {
                    event.isCancelled = true
                    if (player.standby) return
                    player.metadata.kart.invalidate()
//                    player.player.leaveVehicle()
                    return
                }
            }
        }
    }

    // TODO: Implement some caching to prevent allocation/sorting a list every tick
    override fun sortedPlayers() = players.sortedWith(
        compareBy(
            { it.metadata.finishTime < 0 },
            { it.metadata.finishTime },
            { -it.metadata.lap },
            { -it.metadata.trackPointIndex },
            { it.metadata.squaredDistanceFromTrackPoint() }
        )
    )

    override fun canExit(kart: Kart): Boolean = false
    override fun tryToExit(kart: Kart): Boolean = false
    override fun shouldRespawn(kart: Kart): KartRespawnData? = null

    override fun onDestroyed(kart: Kart) {
//        Logger.debug("Kart ${kart.player.name} destroyed")
//        if (state == State.RUNNING) {
//            for (player in players) {
//                if (player.kart == kart && kart.player.isConnected()) {
//                    val trackPoint = player.currentTrackPoint(minigameLevel)
//                    val properties = KartManager.getKartProperties("autopia")!!
//                    val kart = KartManager.startKarting(player.player, properties, this, trackPoint.location.toLocation(player.player.world))
//                    player.kart = kart
//                    return
//                }
//            }
//        }
    }

    fun getKartProperties() = KartManager.kartPropertiesFromConfig(kartId)!!

    override fun update(timePassed: Long) {
        super.update(timePassed)
        if (state == Minigame.State.PREPARING_GAME) {
            if (startingTicks < 3 * 20 && !playedCountdownSound) {
                playedCountdownSound = true
                Bukkit.getWorld("world")!!.playSound(
                    minigameLevel.spawnLocations.first().toLocation(Bukkit.getWorld("world")!!),
                    "${SoundUtils.SOUND_PREFIX}:minigame.autopia.countdown",
                    SoundCategory.AMBIENT,
                    1f,
                    1f
                )
            }
        } else if (state == Minigame.State.RUNNING) {
            for (player in players) {
                if (!player.standby && !KartManager.isKarting(player.player)) {
//                    Logger.debug("Handling new kart request sneaking=${player.player.isSneaking}")
                    val trackPoint = player.metadata.currentTrackPoint(minigameLevel)
                    val nextTrackPoint = player.metadata.nextTrackPoint(minigameLevel)
                    val properties = getKartProperties()

                    try {
                        if (applyColors)
                            properties.kartNpcs.forEach {
                                it.model?.setColor(player.metadata.color.color)
                            }
                        val respawnLocation = trackPoint.location.clone()
                        val yawPitch = LookAtUtil.YawPitch()
                        LookAtUtil.getYawPitchFromRadian(
                            trackPoint.location.toVector(),
                            nextTrackPoint.location.toVector(),
                            yawPitch
                        )
                        respawnLocation.yaw = Math.toDegrees(yawPitch.yaw).toFloat() + 90f

//                        trackPoint.location.spawnParticle<Any>(Particle.LAVA)
//                        nextTrackPoint.location.spawnParticle<Any>(Particle.LAVA)
//
//                        Logger.info("Respawn at ${respawnLocation.yaw.format(2)}")

                        val kart = KartManager.startKarting(
                            player = player.player,
                            kartProperties = properties,
                            kartOwner = this,
                            location = respawnLocation,
                            spawnType = KartManager.SpawnType.Minigame,
                            applySafetyCheck = false,
                        )

                        player.metadata.kart = kart
                    } catch (e: Exception) {
                        logcat(LogPriority.DEBUG) { "Failed to spawn Autopia kart: ${e.message}" }
                    }
                }

                if (!player.standby && (!KartManager.isKarting(player.player) || player.metadata.kart.isParked() || !player.player.isInsideVehicle)) {
//                    Logger.debug("Requesting new kart sneaking=${player.player.isSneaking}")
                    player.player.sendMessage(CVTextColor.serverNotice + "Want to leave this game? use /leave")
                }
            }
            val sortedPlayers = sortedPlayers()
            var allFinished = true

            if (updateDistanceTick > 5) {
                players.forEach { it.metadata.recalculateDistanceFromTrackPoint(minigameLevel) }
                updateDistanceTick = 0
            }
            updateDistanceTick++

            for ((index, player) in sortedPlayers.withIndex()) {
//                if (!player.hasFinished(minigameLevel)) {
                val timeLeft = maxGameTimeLength() - playTime
                val message = "Game ending in ${DateUtils.format(timeLeft, "?")}".let {
                    if (!player.metadata.hasFinished(minigameLevel)) {
                        it + ", estimated position ${(index + 1).asOrdinalAppended()} (lap ${player.metadata.lap} checkpoint ${player.metadata.trackPointIndex})"
                    } else {
                        it + ", you finished ${(index + 1).asOrdinalAppended()}"
                    }
                }
                display(
                    player.player,
                    Message(
                        id = ChatUtils.ID_MINIGAME,
                        text = Component.text(
                            message,
                            CVTextColor.serverNotice
                        ),
                        type = MessageBarManager.Type.MINIGAME,
                        untilMillis = TimeUtils.secondsFromNow(1.0),
                    ),
                    replace = true,
                )

                if (isRunning && !player.standby && player.metadata.finishTime > 0 && player.metadata.finishTime < System.currentTimeMillis() - 3000) {
                    player.standby = true
                    Translation.MINIGAME_PUT_ON_STANDBY.getTranslation(player.player)?.sendTo(player.player)
                    player.metadata.kart.destroy()
                    player.player.teleport(exitLocation)
                    player.player.teleport(exitLocation)
                    GameModeManager.setDefaults(player.player)
                }
//                }

                if (!player.metadata.hasFinished(minigameLevel))
                    allFinished = false
                else {
                    displayTimeLeft
                }
                var trackPoint = player.metadata.nextTrackPoint(minigameLevel)
                var trackpointChanged = false
                while (player.player.location.distanceSquared(trackPoint.location) <= trackPoint.size * trackPoint.size) {
                    if (!player.metadata.moveToNextTrackPoint(this, minigameLevel, index + 1))
                        break
//                    player.player.sendMessage(CVChatColor.COMMAND_GENERAL + "New position: lap=${player.lap} point=${player.trackPointIndex}")
                    trackPoint = player.metadata.nextTrackPoint(minigameLevel)
                    trackpointChanged = true
                }
//                if (trackpointChanged) {
//                    player.player.inventory.getItem(WornItemManager.SLOT_WEAPON)?.let {
//                        updateCompass(player, it)
//                    }
//                }

                if (PluginProvider.isTestServer()) {
                    val particleCount = 12
                    for (i in 0..particleCount) {
                        val workAngle = debugAngle + (Math.toRadians(360.0 / particleCount.toDouble()) * i)
                        val loc = trackPoint.location.clone().add(
                            Vector(
                                cos(workAngle) * trackPoint.size,
                                0.0,
                                sin(workAngle) * trackPoint.size
                            )
                        )
                        player.player.world.spawnParticleX(
                            Particle.REDSTONE,
                            loc.x, loc.y, loc.z,
                            data = Particle.DustOptions(Color.RED, 2f)
                        )
                    }
                }

                if (player.metadata.kart.isValid() && !minigameLevel.area.isInArea(player.metadata.kart.location)) {
                    player.metadata.kart.destroy()
                }
            }

//            debugAngle += Math.toRadians(90.0) / 20.0

            if (allFinished)
                stop(Minigame.StopReason.ALL_PLAYERS_FINISHED)
        }
    }

//    private fun updateCompass(player: MinigamePlayer<AutopiaPlayer>, itemStack: ItemStack): ItemStack {
//        if (!itemStack.hasItemMeta()) return itemStack
//        if (itemStack.itemMeta is CompassMeta)
//            itemStack.updateMeta<CompassMeta> {
//                lodestone = player.metadata.nextTrackPoint(minigameLevel).location
//                if (isLodestoneTracked)
//                    isLodestoneTracked = false
//                if (!hasCustomModelData())
//                    setCustomModelData(1)
//            }
//        return itemStack
//    }
//
//    private fun generateCompassItem(player: MinigamePlayer<AutopiaPlayer>) =
//        updateCompass(player, ItemStack(Material.COMPASS))

    override fun onUpdatePlayerWornItems(player: MinigamePlayer<AutopiaPlayer>, event: PlayerEquippedItemsUpdateEvent) {
        super.onUpdatePlayerWornItems(player, event)

        event.appliedEquippedItems.clearArmor()
        event.appliedEquippedItems.clearSpecials()

        if (state == Minigame.State.PREPARING_GAME || state == Minigame.State.RUNNING) {
            event.appliedEquippedItems.consumptionItem = null
            event.appliedEquippedItems.balloonItem = null
//        event.wornData.miscItem = null
            event.appliedEquippedItems.title = null

//            event.wornData.weaponItem = generateCompassItem(player).toCachedItem()

            if (applyColors) {
                event.appliedEquippedItems.helmetItem =
                    ItemStack(Material.LEATHER_HELMET).setLeatherArmorColor(player.metadata.color.color)
                        .toEquippedItemData()
                event.appliedEquippedItems.chestplateItem =
                    ItemStack(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(player.metadata.color.color)
                        .toEquippedItemData()
                event.appliedEquippedItems.leggingsItem =
                    ItemStack(Material.LEATHER_LEGGINGS).setLeatherArmorColor(player.metadata.color.color)
                        .toEquippedItemData()
                event.appliedEquippedItems.bootsItem =
                    ItemStack(Material.LEATHER_BOOTS).setLeatherArmorColor(player.metadata.color.color)
                        .toEquippedItemData()
            } else {
                event.appliedEquippedItems.helmetItem = null
                event.appliedEquippedItems.chestplateItem = null
                event.appliedEquippedItems.leggingsItem = null
                event.appliedEquippedItems.bootsItem = null
            }
        }
    }

    override fun onPlayerLeft(minigamePlayer: MinigamePlayer<AutopiaPlayer>, reason: Minigame.LeaveReason) {
//        Logger.info("Destroying kart")
        minigamePlayer.metadata.kart.destroy()
        super.onPlayerLeft(minigamePlayer, reason)
    }

    override fun onPreJoin(player: Player) {
        super.onPreJoin(player)
        val index = players.size
        val location = minigameLevel.spawnLocation(index).toLocation(Bukkit.getWorld("world")!!)
        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
        player.inventory.heldItemSlot = EquipmentManager.SLOT_WEAPON
    }

    override fun provideMeta(player: Player): AutopiaPlayer {
        val index = players.size

        val color = if (colors.size == 1) colors[0] else colors[index % (colors.size - 1)]
        val properties = getKartProperties()
        if (applyColors)
            properties.kartNpcs.forEach {
                it.model?.setColor(color.color)
            }
        val kart = KartManager.startKarting(
            player = player,
            kartProperties = properties,
            kartOwner = this,
            location = minigameLevel.spawnLocation(index).toLocation(Bukkit.getWorld("world")!!),
            spawnType = KartManager.SpawnType.Minigame,
        )
        return AutopiaPlayer(player, kart, color)
    }

    override fun onStateChanged(oldState: Minigame.State, newState: Minigame.State) {
        super.onStateChanged(oldState, newState)
        playedCountdownSound = false
        if (newState == Minigame.State.RUNNING) {
            for (player in players) {
                player.metadata.lapStartingTime = System.currentTimeMillis()
            }
        }
    }

    override fun onPreStop(stopReason: Minigame.StopReason) {
        val finishedNormally =
            stopReason == Minigame.StopReason.ALL_PLAYERS_FINISHED || stopReason == Minigame.StopReason.OUT_OF_TIME
        if (finishedNormally) {
            val players = sortedPlayers()

            val winPlayers = players.filter { it.standby }
            winDisplays.forEach { display ->
//                Logger.debug("Display ${display} ${winPlayers.getOrNull(display.index) != null} ${winPlayers.getOrNull(display.index)?.player?.name}")
                val player = winPlayers.getOrNull(display.index)?.player ?: return@forEach
                val entityLocation = display.location.clone().add(0.0, -EntityConstants.ArmorStandHeadOffset, 0.0)
                val entity = entityLocation.chunk.entities.filterIsInstance<ArmorStand>()
                    .filter { it.location.distance(entityLocation) <= 0.05 }.firstOrNull() ?: entityLocation.spawn()
                entity.isVisible = false
                entity.equipment.helmet = player.playerProfile.toSkullItem()
            }

            for (minigamePlayer in players) {
                val player = minigamePlayer.player
                Translation.MINIGAME_WIN_HEADER.getTranslation(player)?.sendTo(player)
                for ((index, otherPlayer) in players.withIndex()) {
                    Translation.MINIGAME_ENTRY_TIMED.getTranslation(
                        player,
                        (index + 1).asOrdinalAppended(),
                        otherPlayer.player.name,
                        if (otherPlayer.metadata.finishTime > 0) DateUtils.formatWithMillis(
                            otherPlayer.metadata.finishTime - playStartTime,
                            "?"
                        ) else "DNF"
                    )?.sendTo(player)
                }
                Translation.MINIGAME_WIN_FOOTER.getTranslation(player)?.sendTo(player)

                executeAsync {
                    val first =
                        player == players.firstOrNull()?.player && minigamePlayer.metadata.hasFinished(minigameLevel)
                    val delta = if (first) rewardWin else rewardPlay
                    MainRepositoryProvider.bankAccountRepository
                        .delta(player.uniqueId, balanceType, delta.toLong(), TransactionType.MINIGAME)
                    when {
                        first -> player.sendMessage(CVTextColor.serverNotice + "+$delta ${balanceType.pluralName} for playing and finishing first")
                        else -> player.sendMessage(CVTextColor.serverNotice + "+$delta ${balanceType.pluralName} for playing")
                    }
                    if (first)
                        MainRepositoryProvider.achievementProgressRepository.increaseCounter(
                            player.uniqueId,
                            "minigame_${internalName}_win"
                        )
                    MainRepositoryProvider.achievementProgressRepository
                        .increaseCounter(player.uniqueId, "minigame_${internalName}_play")
                }
            }
        } else {
            for (minigamePlayer in players) {
                val player = minigamePlayer.player
//                if (stopReason == StopReason.TOO_FEW_PLAYERS) {
                Translation.MINIGAME_TOO_FEW_PLAYERS.getTranslation(player, displayName)?.sendTo(player)
//                } else {
//                    Translation.
//                }
            }
        }
    }

    override fun describeBalanceRewards(): List<Minigame.BalanceReward> {
        return super.describeBalanceRewards() + listOf(
            Minigame.BalanceReward(balanceType, rewardWin, "when you finish 1st"),
            Minigame.BalanceReward(balanceType, rewardPlay, "in any other cases"),
        )
    }

    override fun toJson(): Json = toJson(Json())

    data class KartColor(
        val color: Color,
        val material: ItemStack = MaterialConfig.dataItem(Material.FIREWORK_STAR, 8)
            .setColor(color)
    )

    @JsonClass(generateAdapter = true)
    data class KartColorJson(
        val color: Int,
        val item: String?
    ) {
        fun create() = KartColor(
            color = Color.fromRGB(color and 0x00FFFFFF),
            material = item?.let { ItemStackUtils.fromString(it) } ?: MaterialConfig.dataItem(
                Material.DIAMOND_SWORD,
                -1
            ),
        )
    }

    @JsonClass(generateAdapter = true)
    class Json : BaseMinigame.Json() {
        var colors: Set<KartColorJson> = setOf(
            KartColorJson(colorFromHex("#2c2e8f").asRGB(), null),
            KartColorJson(colorFromHex("#603b1f").asRGB(), null),
            KartColorJson(colorFromHex("#f0ae15").asRGB(), null),
            KartColorJson(colorFromHex("#36393d").asRGB(), null),
            KartColorJson(colorFromHex("#4a5c25").asRGB(), null),
            KartColorJson(colorFromHex("#2389c7").asRGB(), null),
            KartColorJson(colorFromHex("#60aa1b").asRGB(), null),
            KartColorJson(colorFromHex("#e06100").asRGB(), null),
            KartColorJson(colorFromHex("#d5648e").asRGB(), null),
            KartColorJson(colorFromHex("#631f9b").asRGB(), null),
            KartColorJson(colorFromHex("#8f2121").asRGB(), null),
            KartColorJson(colorFromHex("#000000").asRGB(), null)
        )
        lateinit var kartId: String
        var applyColors: Boolean = true
        var rewardAccountType: BankAccountType = BankAccountType.VC

        var winDisplays: List<WinDisplay> = emptyList()

        var rewardWin: Int = 10
        var rewardPlay: Int = 5

        lateinit var level: AutopiaLevel.Json

        override fun createGame() = Autopia(
            id = internalName,
            minigameLevel = level.create(),
            minRequiredPlayers = minRequiredPlayers,
            minKeepPlayingRequiredPlayers = minKeepPlayingRequiredPlayers,
            kartId = kartId,
            name = displayName,
            exitLocation = exitLocation,
            applyColors = applyColors,
            description = description,
            representationItem = ItemStackUtils.fromString(representationItem)
                ?: MaterialConfig.dataItem(Material.DIAMOND_SWORD, -1),
            warpName = warpName,
            rewardWin = rewardWin,
            rewardPlay = rewardPlay,
            balanceType = rewardAccountType,
            colors = colors.map { it.create() }.toTypedArray(),
            saveScores = saveScores,
            winDisplays = winDisplays,
        )
    }

    @JsonClass(generateAdapter = true)
    data class WinDisplay(
        val index: Int,
        val location: Location,
    )
}
