package net.craftventure.core.feature.finalevent

import com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.mojang.authlib.GameProfile
import io.papermc.paper.adventure.PaperAdventure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.plugin.Environment
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvMiniMessage
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.async.executeSyncCancellable
import net.craftventure.core.effect.EffectManager
import net.craftventure.core.extension.getSpectators
import net.craftventure.core.extension.rewardAchievement
import net.craftventure.core.extension.spawn
import net.craftventure.core.feature.balloon.BalloonManager
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.holders.NpcEntityHolder
import net.craftventure.core.feature.casino.CasinoManager
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.extension.utcMillis
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Permissions.DRAGONCLAN
import net.craftventure.core.listener.ChatListener
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.manager.PlayerTimeManager
import net.craftventure.core.manager.TeamsManager
import net.craftventure.core.metadata.CvMetadata
import net.craftventure.core.metadata.PlayerSpecificTeamsMeta
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.FullyManualNpcTracker
import net.craftventure.core.ride.RideManager
import net.craftventure.core.ride.flatride.Flatride
import net.craftventure.core.ride.flatride.Rocktopus
import net.craftventure.core.ride.flatride.Rocktopus.RideSettingsState
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.script.ComposedScript
import net.craftventure.core.script.ScriptManager
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.GameTimeUtils
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.ItemStackUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.repository.PlayerKeyValueRepository.Companion.DISTANCE_TRAVELED_BY_TRAIN
import net.craftventure.database.repository.PlayerKeyValueRepository.Companion.DISTANCE_TRAVELED_BY_TRAM
import net.craftventure.database.type.BankAccountType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minecraft.world.entity.player.PlayerModelPart
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.util.Vector
import org.joml.Matrix4f
import java.io.File
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaDuration

object FinaleCinematic : Listener {
    private val dispatcher = Dispatchers.IO.limitedParallelism(10)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val variableRegex = """\{(?<name>[A-Za-z]+)}""".toRegex()

    private val directory = File(CraftventureCore.getInstance().dataFolder, "data/finale/")

    private var config: FinaleCinematicConfig.MainConfig? = null
    private var scenes = listOf<SceneInstance>()
    private val npcDtos = hashMapOf<String, PlayerRecordingDto>()

    private val npcTracker = FullyManualNpcTracker().apply {
        startTracking()
    }
    private var totalDuration = (0L).toDuration(DurationUnit.MILLISECONDS)
    private var camera: Entity? = null
    private var updater: Int? = null

    private var startedAt = 0L
    private var simulatedTime: Long = GameTimeUtils.hoursMinutesToTicks(12, 0)
    private var simulatedTimeTask: Int? = null

    private var cache = hashMapOf<String, PlayerCacheData>()

    private val timerDisplays = listOf(
        TimerDisplay(
            Location(Bukkit.getWorld("world"), 89.50, 55.12, -868.50, 179.85f, -0.45f),
        ),
        TimerDisplay(
            Location(Bukkit.getWorld("world"), 89.50, 52.24, -599.50, 0.30f, 2.25f),
        )
    )

    fun calculateFinishingIn(): Duration? {
        if (isRunning()) {
            val now = System.currentTimeMillis()
            val finish = startedAt + totalDuration.inWholeMilliseconds

            if (now < finish) {
                return (finish - now).toDuration(DurationUnit.MILLISECONDS)
            }
        }
        return null
    }

    init {
        Bukkit.getServer().pluginManager.registerEvents(this, PluginProvider.getInstance())

        executeSync(20, 20) {
            updateTimerDisplays()
        }
    }

    private fun updateTimerDisplays() {
        timerDisplays.forEach { it.update() }
    }

    fun isRunning(): Boolean {
        return updater != null
    }

    private val players = CopyOnWriteArrayList<Player>()

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        removePlayer(event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerTeleport(playerTeleportEvent: PlayerTeleportEvent) {
        if (isRunning()) {
            if (playerTeleportEvent.cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
                playerTeleportEvent.isCancelled = false
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (isRunning()) {
            executeSync(20 * 2) {
                addPlayer(event.player)
            }
        }
    }

    @EventHandler
    fun onAsyncLogin(event: AsyncPlayerPreLoginEvent) {
        if (isRunning()) {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                CVTextColor.serverNoticeAccent + "A finale event is currently running. Return in ${
                    calculateFinishingIn()?.inWholeMilliseconds?.let { DateUtils.format(it, "?") } ?: "unknown"
                }"
            )
        }
    }

    private fun FinaleCinematicConfig.NpcData.getTeam(): TeamsManager.TeamData {
        return when (this.team) {
            FinaleCinematicConfig.NpcData.TeamType.crew -> TeamsManager.crewTeam()
            FinaleCinematicConfig.NpcData.TeamType.vip -> TeamsManager.vipTeam()
            null -> TeamsManager.getTeamDataFor(NamedTextColor.GOLD)
        }
    }

    private fun addPlayer(player: Player): Boolean {
        if (player !in players) {
            camera?.location?.let { player.teleport(it) }

            players.add(player)
            npcTracker.addPlayer(player)

            scope.launch(dispatcher) {
                cachePlayer(player)
            }
            player.gameMode = GameMode.SPECTATOR
            player.spectatorTarget = camera
            return true
        }
        return false
    }

    private fun removePlayer(player: Player): Boolean {
        if (player in players) {
            players.remove(player)
            PlayerTimeManager.reset(player)

            player.getMetadata<PlayerSpecificTeamsMeta>()?.apply {
                scenes.forEach {
                    it.playbacks?.forEach { playback ->
                        if (playback.config.hideNameTag)
                            remove(playback.npc)
                    }
                }
            }
            npcTracker.removePlayer(player)
            GameModeManager.setDefaults(player)
            player.teleport(player.world.spawnLocation)

            player.rewardAchievement("cv_finale")
            if (player.isCrew() && CraftventureCore.getInstance().environment == Environment.DEVELOPMENT)
                player.sendMessage(CVTextColor.serverNoticeAccent + "Non-crew kicked (not on dev)")
            else
                player.kick(
                    Translation.ACHIEVEMENT_REWARD.getTranslation(
                        player,
                        "The Last Ride",
                        "After 12 years since opening to guests, Craftventure has come to its conclusion.\n" +
                                "You witnessed the end of Craftventure in an epic cinematic credits sequence straight into the final sunset.\n" +
                                "Goodbye and thanks for all the fish.\n" +
                                "RIP Craftventure, 2012-2024",
                        31,
                        "VC"
                    )!! + (CVTextColor.serverNoticeAccent + "\n \n \nNote: Unless this was the final-final cinematic, you can rejoin to watch it again")
                )
            return true
        }
        return false
    }

    private fun cancel() {
        Bukkit.getWorlds().forEach {
            it.time = GameTimeUtils.hoursMinutesToTicks(12, 0)
        }
        updater?.let { Bukkit.getScheduler().cancelTask(it) }
        updater = null

        if (!ChatListener.chatEnabled) {
            Bukkit.broadcast(CVTextColor.serverNoticeAccent + "The moment of silence is now over")
        }
        ChatListener.chatEnabled = true
        cache.clear()
        players.forEach(::removePlayer)
        TitleManager.disabled = false
        simulatedTimeTask?.let { Bukkit.getScheduler().cancelTask(it) }

        camera?.remove()
        camera = null

        currentSceneInstance?.destroy()

        config?.audioAreaName?.let { AudioServerApi.disable(it) }
        players.clear()

        FinaleTimer.onCinematicCancelled()
    }

    fun reload() {
        try {
            cancel()
            players.clear()
            npcDtos.clear()
            this.scenes.forEach { it.destroy() }


            File(directory, "npc").walkTopDown().filter { it.isFile && it.extension == "json" }.forEach {
                npcDtos[it.name] = CvMoshi.adapter(PlayerRecordingDto::class.java).fromJson(it.readText())!!
            }

            val config = File(directory, "script.json").readText().let {
                CvMoshi.adapter(FinaleCinematicConfig.MainConfig::class.java).fromJson(it)!!
            }
            val sceneConfigs = config.sceneList.map {
                File(directory, "scenes/${it}.json").readText().let {
                    CvMoshi.adapter(FinaleCinematicConfig.SceneConfig::class.java).fromJson(it)!!
                } to it
            }
            this.config = config
            var currentDuration = 0L.toDuration(DurationUnit.MILLISECONDS)
            this.scenes = sceneConfigs.map { (config, name) ->
                val endsAt = currentDuration + config.duration.duration
                val result = SceneInstance(name, config, currentDuration, endsAt)
                currentDuration = endsAt
                return@map result
            }
            this.totalDuration =
                this.scenes.sumOf { it.config.duration.duration.inWholeMilliseconds }
                    .toDuration(DurationUnit.MILLISECONDS)

            logcat { "Loaded cinematic with ${scenes.size}" }
            scenes.forEach {
                logcat {
                    "  - Scene ${it.name} ${
                        DateUtils.format(
                            it.startsAt.inWholeMilliseconds,
                            ""
                        )
                    } to ${DateUtils.format(it.endsAt.inWholeMilliseconds, "")}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logcat { "Failed to load finale" }
            npcDtos.clear()
            scenes = emptyList()
        }
    }

    private fun cachePlayer(player: Player) {
        cache[player.name] = PlayerCacheData().apply {
            val ownedItems = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId)

            val unlockedAchievements =
                MainRepositoryProvider.achievementProgressRepository.findByPlayer(player.uniqueId)

            val cvMetadata = player.getMetadata<CvMetadata>()
            val guestStat = MainRepositoryProvider.guestStatRepository.find(player.uniqueId)

            this.data["name"] = { player.name }
            val guestId = MainRepositoryProvider.guestStatRepository.getRowId(player.uniqueId)

            this.data["uniqueNumber"] = { guestId?.toString() }

            this.data["ownedItemCount"] =
                { ownedItems.size.toString() }

            this.data["achievementUnlockedCount"] =
                { unlockedAchievements.size.toString() }

            this.data["vcBalance"] =
                {
                    (MainRepositoryProvider.bankAccountRepository.get(player.uniqueId, BankAccountType.VC)?.balance
                        ?: 0L).toString()
                }

            this.data["isDragonclan"] =
                { if (player.hasPermission(DRAGONCLAN)) "joined DragonClan" else "never joined DragonClan" }

            this.data["distanceByTrain"] =
                {
                    val value = MainRepositoryProvider.playerKeyValueRepository.getValue(
                        player.uniqueId,
                        DISTANCE_TRAVELED_BY_TRAIN
                    )?.toDoubleOrNull()

                    if (value != null) {
                        if (value > 1000) {
                            "${(value / 1000).format(1)}km"
                        } else {
                            "${value.roundToInt()}m"
                        }
                    } else null
                }

            this.data["distanceByTram"] =
                {
                    val value = MainRepositoryProvider.playerKeyValueRepository.getValue(
                        player.uniqueId,
                        DISTANCE_TRAVELED_BY_TRAM
                    )
                        ?.toDoubleOrNull()

                    if (value != null) {
                        if (value > 1000) {
                            "${(value / 1000).format(1)}km"
                        } else {
                            "${value.roundToInt()}m"
                        }
                    } else null
                }

            this.data["totalRideCount"] =
                {
                    MainRepositoryProvider.rideCounterRepository.findAllForUuid(player.uniqueId)
                        .sumOf { it.count ?: 0 }
                        .toString()
                }

            if (guestStat != null) {
                this.data["firstSeenDate"] =
                    {
                        guestStat.firstSeen?.let {
                            "${it.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())} " +
                                    "${it.dayOfMonth}, ${it.year}"
                        }
                    }
                this.data["totalAfkTime"] =
                    {
                        DateUtils.format(guestStat.totalAfkTime!! * 1000L, "?")
                    }

                this.data["totalOnlineTime"] =
                    {
                        val totalOnline = cvMetadata?.currentTotalOnlineTimeInMs ?: 0L
                        DateUtils.format(totalOnline, "?")
                    }

                this.data["totalPlayTime"] =
                    {
                        val totalActive = cvMetadata?.currentActiveOnlineTimeInMs ?: 0L
                        DateUtils.format(totalActive, "?")
                    }
            }

//            logcat { "Cached ${player.name} with ${this.data.keys}" }
        }
    }

    fun prepareStart() {
        ChatListener.chatEnabled = false
        Bukkit.broadcast(CVTextColor.serverNoticeAccent + "Let's enjoy a moment of silence")
        RideManager.allowAutoDispatch = false
        TitleManager.disabled = true

        executeSync((20 * 2L) + 2) {
            Bukkit.getOnlinePlayers().forEach { it.teleport(it.world.spawnLocation) }
        }

        broadcastTitle(
            title = Component.text("\uE06B", NamedTextColor.BLACK),
            times = Title.Times.times(
                (2).seconds.toJavaDuration(),
                java.time.Duration.ofSeconds(4),
                java.time.Duration.ofSeconds(2),
            ),
            players = Bukkit.getOnlinePlayers(),
        )
        FinaleTimer.startFinaleMode()
    }

    fun start() {
        if (scenes.isEmpty()) return
        if (isRunning()) return

        simulatedTime = GameTimeUtils.hoursMinutesToTicks(12, 0)

        timerDisplays.forEach { it.removeEntity() }

        cache.clear()

        Bukkit.getOnlinePlayers().forEach(::addPlayer)

        camera?.remove()
        camera = null

        AudioServerApi.sync(
            config!!.audioAreaName,
            System.currentTimeMillis() - (config?.startAt?.inWholeMilliseconds ?: 0L)
        )
        AudioServerApi.enable(config!!.audioAreaName)

        startedAt = System.currentTimeMillis() - (config?.startAt?.inWholeMilliseconds ?: 0L)
        previousTime = 0
        var camFix = 0

        updater = executeSync(1, 1) {
            update()

            players.forEach { player ->
                if (player.gameMode != GameMode.SPECTATOR) {
                    player.gameMode = GameMode.SPECTATOR
                    player.spectatorTarget = camera
                }
            }

            // Nasty workaround to "fix" world loading
            camFix++
            if (camFix % 20 == 0) {
                camera?.let {
                    it.getSpectators().forEach {
                        it.spectatorTarget = null
                        it.spectatorTarget = camera
                    }
                }
            }
        }

        currentSceneInstance = scenes.first()
        currentSceneInstance!!.create(0)

        config?.initialActions?.forEach { execute(it) }

        respawnCamera(
            currentSceneInstance!!.config.cameraPaths.first().cameraFrames.first().location,
            currentSceneInstance!!.config.cameraType
        )
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        if (isRunning()) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onStopSpectate(event: PlayerStopSpectatingEntityEvent) {
        if (isRunning() && event.spectatorTarget === camera) {
            event.isCancelled = true
        }
    }

    private fun respawnCamera(location: Location, type: EntityType) {
        val newCam = location.world.spawnEntity(location, type)
        if (newCam is LivingEntity) {
            newCam.isInvisible = true
        }
//        newCam.interpolationDuration = 2
//        newCam.interpolationDelay = 0
        players.forEach {
            if (it.gameMode != GameMode.SPECTATOR) {
                it.gameMode = GameMode.SPECTATOR
            }
            it.spectatorTarget = null
            EntityUtils.teleport(it, newCam.location)
            it.spectatorTarget = newCam
        }
        camera?.remove()
        camera = newCam
    }

    private var currentSceneInstance: SceneInstance? = null
    private var previousCameraPath: FinaleCinematicConfig.CameraPath? = null

    private var previousTime = 0L
    private fun update() {
        val currentTime = System.currentTimeMillis() - startedAt
        val currentDuration = currentTime.toDuration(DurationUnit.MILLISECONDS)

        if (currentDuration > totalDuration) {
//            logcat { "Cancel, finished" }
            cancel()
            return
        }

        if (currentSceneInstance == null || currentDuration > currentSceneInstance!!.endsAt) {
            val nextInstance = scenes.lastOrNull { currentDuration > it.startsAt }
            if (nextInstance != null) {
//                logcat { "Next scene ${nextInstance.endsAt} (was ${currentSceneInstance?.endsAt})" }
                currentSceneInstance?.destroy()
                simulatedTime = GameTimeUtils.hoursMinutesToTicks(12, 0)
                nextInstance.create(currentDuration.inWholeMilliseconds - nextInstance.startsAt.inWholeMilliseconds)
                syncSimulatedTime()
                currentSceneInstance = nextInstance

                respawnCamera(
                    nextInstance.config.cameraPaths.first().cameraFrames.first().location,
                    nextInstance.config.cameraType
                )
            }
        }

        config?.timedActions?.forEach {
            if (it.at.inWholeMilliseconds in previousTime..<currentTime) {
                execute(it.action)
            }
        }

        val currentSceneInstance = currentSceneInstance
        if (currentSceneInstance != null) {
            val durationIntoScene = currentDuration - currentSceneInstance.startsAt
            val previousDurationInScene =
                durationIntoScene - (currentTime - previousTime).toDuration(DurationUnit.MILLISECONDS)

            var currentDurationOffset = Duration.ZERO

            currentSceneInstance.config.timedActions?.forEach {
                if (it.at.duration in previousDurationInScene..<durationIntoScene) {
//                    logcat { "Execute ${it.at} beacuse ${previousDurationInScene}-$durationIntoScene" }
                    execute(it.action)
                }
            }

            for (path in currentSceneInstance.config.cameraPaths) {
                if (durationIntoScene < currentDurationOffset + path.calculatedDuration) {
                    val start =
                        path.cameraFrames.lastOrNull { it.at.duration <= durationIntoScene - currentDurationOffset }
                    val end =
                        path.cameraFrames.firstOrNull { it.at.duration > durationIntoScene - currentDurationOffset }

                    if (start != null && end != null) {
                        val percentage =
                            ((durationIntoScene - start.at.duration) / (end.at.duration - start.at.duration)).coerceIn(
                                0.0,
                                1.0
                            )
//                        logcat { "Interpolate $percentage: ${start.at.duration} to ${end.at.duration}" }
                        val x = InterpolationUtils.linearInterpolate(start.location.x, end.location.x, percentage)
                        val y = InterpolationUtils.linearInterpolate(start.location.y, end.location.y, percentage)
                        val z = InterpolationUtils.linearInterpolate(start.location.z, end.location.z, percentage)
                        val yaw = InterpolationUtils.linearInterpolate(
                            start.location.yaw.toDouble(),
                            end.location.yaw.toDouble(),
                            percentage
                        )
                        val pitch = InterpolationUtils.linearInterpolate(
                            start.location.pitch.toDouble(),
                            end.location.pitch.toDouble(),
                            percentage
                        )
                        moveCamera(
                            currentSceneInstance.config,
                            path,
                            Location(start.location.world, x, y, z, yaw.toFloat(), pitch.toFloat())
                        )
                    } else if (start != null) {
//                        logcat { "Start ${start.at.duration}" }
                        moveCamera(currentSceneInstance.config, path, start.location)
                    } else if (end != null) {
//                        logcat { "End ${end.at.duration}" }
                        moveCamera(currentSceneInstance.config, path, end.location)
                    }

//                    path.cameraFrames.forEach { frame ->
//                        if (currentDurationOffset + frame.at > currentDuration)
//                            previousFrame = frame
//                    }
//                    targetLocation = path.cameraFrames.logcat { "Percentage $percentage" }
                }
                currentDurationOffset += path.calculatedDuration
            }
        }

        previousTime = currentTime
    }

    private fun broadcastTitle(
        title: Component?,
        subtitle: Component? = null,
        times: Title.Times,
        players: Iterable<Player> = this.players
    ) {
        val titleData = Title.title(
            title ?: Component.empty(),
            subtitle ?: Component.empty(),
            times
        )

        players.forEach {
            it.showTitle(titleData)
        }
    }

    private fun execute(action: FinaleCinematicConfig.SceneAction) {
        when (action) {
            is FinaleCinematicConfig.SceneAction.FadeEnd -> broadcastTitle(
                title = Component.text("\uE06B", NamedTextColor.BLACK),
                times = Title.Times.times(
                    java.time.Duration.ofMillis(0),
                    java.time.Duration.ofMillis(0),
                    action.duration.duration.toJavaDuration(),
                ),
            )

            is FinaleCinematicConfig.SceneAction.FadeStart -> broadcastTitle(
                title = Component.text("\uE06B", NamedTextColor.BLACK),
                times = Title.Times.times(
                    action.duration.duration.toJavaDuration(),
                    java.time.Duration.ofSeconds(2),
                    java.time.Duration.ofSeconds(2),
                ),
            )

            is FinaleCinematicConfig.SceneAction.SetTime -> {
                simulatedTime = GameTimeUtils.hoursMinutesToTicks(action.hour, action.minute)
                syncSimulatedTime()
            }

            is FinaleCinematicConfig.SceneAction.AnimateTime -> {
                val transition = PlayerTimeManager.getBestTransition(
                    GameTimeUtils.hoursMinutesToTicks(action.startHour, action.startMinute),
                    GameTimeUtils.hoursMinutesToTicks(action.endHour, action.endMinute),
                )
                simulatedTimeTask?.let { Bukkit.getScheduler().cancelTask(it) }
                val syncStart = System.currentTimeMillis()

                simulatedTimeTask = executeSync(0, 1) {
                    val syncNow = System.currentTimeMillis()
                    val progress = (syncNow - syncStart) / action.fadeTime.inWholeMilliseconds.toDouble()

//                    logcat { "Progress $progress" }

                    if (progress >= 1.0) {
                        simulatedTime = transition.last
                        syncSimulatedTime()
                        simulatedTimeTask?.let { Bukkit.getScheduler().cancelTask(it) }
                    } else {
                        simulatedTime =
                            (transition.first + (transition.last - transition.first).times(progress)).toLong()
                        syncSimulatedTime()
                    }
                }
            }

            is FinaleCinematicConfig.SceneAction.PlaySpecialEffect ->
                EffectManager.findByName(action.effect)?.play()

            is FinaleCinematicConfig.SceneAction.ScriptStart ->
                ScriptManager.getScriptController(
                    action.group,
                    action.scene
                )?.start()

            is FinaleCinematicConfig.SceneAction.AssumeRunning ->
                ScriptManager.getScriptController(
                    action.group,
                    action.scene
                )?.apply {
                    if (start()) {
                        config?.startAt?.let { startAt ->
                            this.scripts.filterIsInstance<ComposedScript>()
                                .forEach { it.setStartTimeTo(startedAt + action.startedAt.inWholeMilliseconds) }
                        }
                    }
                }

            is FinaleCinematicConfig.SceneAction.ScriptStop ->
                ScriptManager.getScriptController(
                    action.group,
                    action.scene
                )?.stop()

            is FinaleCinematicConfig.SceneAction.DispatchRide -> RideManager.getRide(action.ride)?.let { ride ->
                if (ride is OperableCoasterTrackedRide) {
                    val segment =
                        if (action.station != null) ride.getSegmentById(action.station) else ride.trackSegments.firstOrNull { it is StationSegment }
                    if (segment is StationSegment) {
                        if (!segment.tryDispatchNow(StationSegment.DispatchRequestType.AUTO)) {
//                        segment.tryDispatchNow(StationSegment.DispatchRequestType.OPERATOR)
                        }
                    }
                } else if (ride is Flatride<*>) {
                    if (ride.canStart()) {
                        ride.start()

                        if (ride is Rocktopus) {
                            ride.overrideRideSettings(
                                RideSettingsState(
                                    name = "Ground spinner",
                                    rideSpeed = 1,
                                    armSpeed = 0,
                                    discSpeed = 2,
                                    armDegrees = 0
                                )
                            )
                        }
                    }
                }
            }

            is FinaleCinematicConfig.SceneAction.FadeInText -> {
                val text = currentSceneInstance?.textInstances?.find {
                    it.config.id == action.id
                }
                if (text != null) {
                    val start = System.currentTimeMillis()
                    executeSyncCancellable(0, 1) {
                        val now = System.currentTimeMillis()
                        val progress =
                            ((now - start) / action.duration.inWholeMilliseconds.toDouble()).coerceIn(0.0, 1.0)
                        text.entity?.textOpacity = calculateOpacity(progress)
//                        logcat { "$progress >> ${calculateOpacity(progress)}" }
                        if (progress >= 1.0) {
                            text.entity?.textOpacity = calculateOpacity(1.0)
                            cancel()
                        }
                    }
                }
            }

            is FinaleCinematicConfig.SceneAction.FadeOutText -> {
                val text = currentSceneInstance?.textInstances?.find {
                    it.config.id == action.id
                }
                if (text != null) {
                    val start = System.currentTimeMillis()
                    executeSyncCancellable(0, 1) {
                        val now = System.currentTimeMillis()
                        val progress =
                            ((now - start) / action.duration.inWholeMilliseconds.toDouble()).coerceIn(0.0, 1.0)
                        text.entity?.textOpacity = calculateOpacity(1 - progress)
                        if (progress >= 1.0) {
                            text.entity?.textOpacity = calculateOpacity(0.0)
                            cancel()
                        }
                    }
                }
            }

            is FinaleCinematicConfig.SceneAction.HardcodedAction -> when (action.action) {
                FinaleCinematicConfig.SceneAction.HardcodedAction.Type.SpinWheelOfFortune -> CasinoManager.wheelOfFortune?.play()
            }

            is FinaleCinematicConfig.SceneAction.Eating -> {
                val who = currentSceneInstance?.playbacks?.find { it.config.id == action.who }?.npc ?: return
                who.setByteFlag(
                    EntityMetadata.LivingEntity.flags,
                    EntityMetadata.LivingEntity.flags.resolveFlag("isUsing")!!,
                    action.eating
                )
            }

            is FinaleCinematicConfig.SceneAction.Mount -> {
                val passenger = currentSceneInstance?.playbacks?.find { it.config.id == action.passenger }!!
                val seat = currentSceneInstance?.playbacks?.find { it.config.id == action.seat }!!

                passenger.npc.mount(seat.npc.entityId)
            }

            is FinaleCinematicConfig.SceneAction.Sit -> {
                val passenger = currentSceneInstance?.playbacks?.find { it.config.id == action.who } ?: return
                val seat = passenger.playback.lastLocation.spawn<ItemDisplay>()
                passenger.npc.mount(seat.entityId)
                currentSceneInstance!!.entities.add(seat)
            }
        }
    }

    private fun syncSimulatedTime() {
        Bukkit.getWorlds().forEach {
            it.time = simulatedTime
        }
    }

    private fun moveCamera(
        config: FinaleCinematicConfig.SceneConfig,
        cameraPath: FinaleCinematicConfig.CameraPath,
        location: Location
    ) {
        if (previousCameraPath !== cameraPath) {
            respawnCamera(location, config.cameraType)
        } else
            camera?.let { EntityUtils.teleport(it, location) }

        previousCameraPath = cameraPath

        currentSceneInstance?.update(location)
    }

    private class SceneInstance(
        val name: String,
        val config: FinaleCinematicConfig.SceneConfig,
        val startsAt: Duration,
        val endsAt: Duration,
    ) {
        val entities = mutableListOf<Entity>()
        val itemInstances = config.itemDisplays?.map {
            ItemInstance(it)
        }
        val textInstances = config.textDisplays?.map {
            TextInstance(it)
        }
        val playbacks = config.npcs?.map {
            PlayerPlaybackInstance(it, npcDtos[it.npcFileName]!!)
        }

        fun create(offset: Long) {
            logcat { "Start scene with $offset" }
            config.initialActions?.forEach { execute(it) }
            itemInstances?.forEach { it.create() }
            textInstances?.forEach { it.create() }
            playbacks?.forEach {
                npcTracker.addEntity(it.npc)
                it.playback.start(it.config.startPlayingAt.inWholeMilliseconds + offset)
                it.create()
            }

            players.forEach { player ->
                player.getMetadata<PlayerSpecificTeamsMeta>()?.apply {
                    playbacks?.forEach { playback ->
                        if (playback.config.hideNameTag) {
                            addOrUpdate(playback.npc, playback.config.getTeam())
                        }
                    }
                }
            }
        }

        fun update(camLocation: Location) {
            itemInstances?.forEach { it.update(camLocation) }
            textInstances?.forEach { it.update(camLocation) }
        }

        fun destroy() {
            entities.forEach { it.remove() }
            entities.clear()

            itemInstances?.forEach { it.destroy() }
            textInstances?.forEach { it.destroy() }
            playbacks?.forEach {
                it.destroy()
                it.playback.stop()
                npcTracker.removeEntity(it.npc)
            }

            players.forEach { player ->
                player.getMetadata<PlayerSpecificTeamsMeta>()?.apply {
                    playbacks?.forEach { playback ->
                        if (playback.config.hideNameTag) {
                            remove(playback.npc)
                        }
                    }
                }
            }
        }
    }

    private class ItemInstance(
        val config: FinaleCinematicConfig.ItemDisplayConfig,
    ) {
        private var entity: ItemDisplay? = null

        fun create() {
            entity = config.initialLocation.spawn<ItemDisplay>().apply {
                viewRange = 100f
                brightness = Display.Brightness(15, 15)
                itemStack = config.model.let { ItemStackUtils.fromString(it) }
//                interpolationDuration = 1
                setTransformationMatrix(Matrix4f().scale(config.initialScale.toFloat()))
            }
        }

        fun update(camLocation: Location) {
            config.followCamDistance?.let {
                entity?.teleport(
                    camLocation.clone().add(camLocation.direction.normalize().multiply(it))
                        .add(0.0, EntityConstants.ArmorStandHeadOffset, 0.0)
                )
            }
        }

        fun destroy() {
            entity?.remove()
            entity = null
        }
    }

    private class TextInstance(
        val config: FinaleCinematicConfig.TextDisplayConfig,
    ) {
        var entity: TextDisplay? = null
        var update = 0

        fun create() {
            entity = config.initialLocation.spawn<TextDisplay>().apply {
                viewRange = 100f
                brightness = Display.Brightness(15, 15)
                alignment = config.alignment
                billboard = config.billboard
                lineWidth = 2000

                if (config.useDefaultBackground)
                    isDefaultBackground = true
                else
                    backgroundColor = Color.fromARGB(0, 0, 0, 0)
//                interpolationDuration = 1
                setTransformationMatrix(Matrix4f().scale(config.initialScale.toFloat()))
                textOpacity = calculateOpacity(config.initialOpacity)
            }

            updateText()

            if (config.useVariables && !config.updateTitleEverySecond) {
                executeSync(5) { updateText() }
            }
        }

        private fun updateText() {
//            logcat { "Update text?" }
            entity?.apply {
                val rawText = config.text

                if (config.useVariables) {

                    players.forEach { player ->

                        val data = cache[player.name]
                        if (data != null) {
                            val wrapperPlayServerEntityMetadata = WrapperPlayServerEntityMetadata()
                            wrapperPlayServerEntityMetadata.entityID = this.entityId

                            val changed = variableRegex.replace(rawText) {
//                                logcat { "Replacing ${it.value}" }
                                it.groups["name"]?.let { data.data[it.value]?.invoke() } ?: "?"
                            }
//                            logcat { "Replaced to $changed" }
                            val parsedText = CvMiniMessage.deserialize(changed)
                            val vanillaText = (if (config.font != null)
                                parsedText.style(parsedText.style().font(Key.key("minecraft", config.font)))
                            else
                                parsedText).let { PaperAdventure.asVanilla(it) }

                            wrapperPlayServerEntityMetadata.metadata = listOf(
                                WrappedDataValue(
                                    EntityMetadata.TextDisplay.text.absoluteIndex!!,
                                    EntityMetadata.TextDisplay.text.wrappedSerializer,
                                    vanillaText,
                                )
                            )
                            wrapperPlayServerEntityMetadata.sendPacket(player)
                        }
                    }
                } else {
                    val parsedText = CvMiniMessage.deserialize(rawText)
                    if (config.font != null)
                        text(parsedText.style(parsedText.style().font(Key.key("minecraft", config.font))))
                    else
                        text(parsedText)
                }
            }
        }

        fun update(camLocation: Location) {
            config.followCamDistance?.let {
                entity?.teleport(
                    camLocation.clone().add(camLocation.direction.normalize().multiply(it))
                        .add(0.0, EntityConstants.ArmorStandHeadOffset, 0.0)
                        .add(config.followOffset ?: Vector())
                )
            }
            if (config.updateTitleEverySecond) {
                update++
                if (update >= 20) {
                    updateText()
                    update = 0
                }
            }
        }

        fun destroy() {
            entity?.remove()
            entity = null
        }
    }

    fun calculateOpacity(opacity: Double): Byte {
        return opacity.times(255).toInt().let { if (it > 127) it - 256 else it }.toByte().let {
            if (it >= 0 && it <= 4.toByte()) return@let 5
            return@let it
        }
    }

    private class PlayerPlaybackInstance(
        val config: FinaleCinematicConfig.NpcData,
        val data: PlayerRecordingDto
    ) {
        val npc = NpcEntity(
            entityType = config.entityType,
            location = data.initialActions.filterIsInstance<PlayerRecordingActionDto.Location>().first().location,
            profile = config.profile?.let { MainRepositoryProvider.cachedGameProfileRepository.findCached(it)!! },
        )
        val balloonData = config.balloon?.let {
            MainRepositoryProvider.ownableItemRepository.findCached(it)
        }?.let { BalloonManager.toBalloon(it) }
        val holder = NpcEntityHolder(BalloonHolder.TrackerInfo(npcTracker, false), npc, offset = Vector(0.0, 1.2, 0.0))

        init {
            if (config.isSelf) {
                npc.playerSpecificGameProfileProvider = { player ->
                    player.playerProfile.let { CraftPlayerProfile.asAuthlib(it) }.let { source ->
                        GameProfile(npc.uuid, source.name).also {
                            it.properties.putAll(source.properties)
                        }
                    }
                }
            }

            config.displayName?.let {
                val component = CvMiniMessage.deserialize(it)
//                if (config.entityType == EntityType.PLAYER)
//                    npc.profileOverridenDisplayName = PaperAdventure.asVanilla(component)
//                else {
                npc.customName(component)
                npc.customNameVisible(true)
//                }
            }

            config.model?.let { npc.helmet(ItemStackUtils.fromString(it)) }
            if (config.entityType == EntityType.ARMOR_STAND) {
                npc.hideBasePlate(true)
                npc.invisible(true)
            }

            PlayerModelPart.entries.forEach { part ->
                npc.setByteFlag(
                    EntityMetadata.Player.customization,
                    part.mask.toByte(),
                    true
                )
            }
        }

        fun create() {
            balloonData?.let {
                BalloonManager.create(holder, it)
            }
        }

        fun destroy() {
            BalloonManager.remove(holder)
        }

        val playback = PlayerRecordingPlayback(npc, data)
    }

    private class PlayerCacheData {
        var data = hashMapOf<String, () -> String?>()
    }

    class TimerDisplay(
        val location: Location,
        var entity: TextDisplay? = null,
    ) {
        fun removeEntity() {
            entity?.remove()
            entity = null
        }

        fun update() {
            if (isRunning()) {
                removeEntity()
                return
            }

            val isNext = FinaleTimer.nextPlay != null && FinaleTimer.nextPlay != FinaleTimer.endingDate
            val diff = (FinaleTimer.nextPlay ?: FinaleTimer.endingDate).utcMillis - LocalDateTime.now().utcMillis

            if (diff < 0) {
                removeEntity()
                return
            }

            if (entity?.isValid != true) {
                removeEntity()
            }
            val entity = entity ?: location.spawn<TextDisplay>().apply {
                viewRange = 100f
                brightness = Display.Brightness(15, 15)
                alignment = TextDisplay.TextAlignment.CENTER
                billboard = Display.Billboard.CENTER
                lineWidth = 2000

                backgroundColor = Color.fromARGB(0, 0, 0, 0)
//                interpolationDuration = 1
                setTransformationMatrix(Matrix4f().scale(8f))
            }
            this.entity = entity

            val parsedText =
                "<server_notice_accent>${if (isNext) "Next finale" else "Finale"} event starting in ${
                    DateUtils.format(
                        diff,
                        "now!"
                    )
                }".parseWithCvMessage()

            entity.text(parsedText.style(parsedText.style().font(Key.key("minecraft", "minguarana"))))
        }
    }
}