package net.craftventure.core.ride.flatride

import com.comphenix.packetwrapper.WrapperPlayServerEntityHeadRotation
import com.comphenix.packetwrapper.WrapperPlayServerEntityTeleport
import com.comphenix.protocol.ProtocolLibrary
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.neatmonster.nocheatplus.checks.CheckType
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo
import fr.neatmonster.nocheatplus.hooks.AbstractNCPHook
import fr.neatmonster.nocheatplus.hooks.NCPHookManager
import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.entitymeta.removeMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.animation.interpolation.*
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.extension.random
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.manager.PlayerTimeManager
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.npc.strikeLightningEffect
import net.craftventure.core.ride.trackedride.FlatrideManager
import net.craftventure.core.ride.trackedride.SplineHandle
import net.craftventure.core.ride.trackedride.SplineNode
import net.craftventure.core.ride.trackedride.TrackedRideManager
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.SplinedTrackSegment
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.script.ScriptManager
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.EntityUtils.setInstantUpdate
import net.craftventure.core.utils.FireworkUtils
import net.craftventure.core.utils.GameTimeUtils
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.extension.decodeBase64ToByteArray
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import penner.easing.Linear
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.collections.set


class Soarin private constructor() : Flatride<Soarin.SoarinBench>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    "soarin",
    "ride_soarin"
) {
    private val preshowArea = SimpleArea("world", )
    private var railsOffset = 0.0
    private val position = Vector(0, 0, 0)
    private val stationSegment: SplinedTrackSegment
    private var showController: ShowController

    private val rideDoor = arrayOf(
        Location(Bukkit.getWorld("world"), )
    )

    private val enterDoors = arrayOf(
        Location(Bukkit.getWorld("world"), ),
        Location(Bukkit.getWorld("world"), )
    )
    private val exitDoors = arrayOf(
        Location(Bukkit.getWorld("world"),),
        Location(Bukkit.getWorld("world"),),
        Location(Bukkit.getWorld("world"),),
        Location(Bukkit.getWorld("world"),)
    )

    private var config: SoarinConfig? = null
        set(value) {
            field = value
            updateFlyRoute()
        }
    private var flyRoute: Array<SoarinPathPart> = emptyArray()
    private var currentFlyRoute: Array<SoarinPathPart> = emptyArray()
    private var flyingStartTime: Long = 0
    private var currentSplineIndex = 0
    private val players = HashSet<Player>()
    private var flying: ArmorStand? = null

    private var state = State.STARTING
    private var frame = 0
    private var ticksInState = 0

    private var cacheLocation: Location = Location(area.world, 0.0, 0.0, 0.0)
    private val cachePosition = Position()
    private var lastPathIndex = 0
    private var isRespawningCamera = false
    private var isLinkingPlayers = false
    private var lastUpdateTime = 0L
    private val timedRideDispatchRequests = arrayOf(
        TimedRideDispatchRequest(3.0, "hyperion"),
        TimedRideDispatchRequest(2.0, "spacemountain", "station1"),
        TimedRideDispatchRequest(2.0, "spacemountain", "station2"),
        TimedRideDispatchRequest(21.0, "ccr"),
        TimedRideDispatchRequest(65.5, "fenghuang"),
        TimedRideDispatchRequest(70.0, "swingride"),
        TimedRideDispatchRequest(74.5, "fenrir"),
        TimedRideDispatchRequest(50.0, "eaglesfury"), // Not visible that far
        TimedRideDispatchRequest(225.0 - 25.0, "alphadera"),
    )
    private var fireworkSpawns = arrayOf<FireworkTrigger>()

    private val ccrRange = PlayerTimeManager.getBestTransition(
        GameTimeUtils.hoursMinutesToTicks(18, 0),
        GameTimeUtils.hoursMinutesToTicks(20, 0)
    )
    private val timeRangeCastle = PlayerTimeManager.getBestTransition(
        GameTimeUtils.hoursMinutesToTicks(7, 30),
        GameTimeUtils.hoursMinutesToTicks(4, 0)
    )

    private val playerTimeSpans = arrayOf(
        PlayerTimeControlSpan(59.5, 62.5) { at, span ->
            Linear.easeNone(
                at - span.start,
                ccrRange.start.toDouble(),
                (ccrRange.endInclusive - ccrRange.start).toDouble(),
                span.end!! - span.start
            ).toLong()
        },
        PlayerTimeControlSpan(135.0, 145.0) { at, span ->
            Linear.easeNone(
                at - span.start,
                timeRangeCastle.start.toDouble(),
                (timeRangeCastle.endInclusive - timeRangeCastle.start).toDouble(),
                span.end!! - span.start
            ).toLong()
        }
    )
    private val thunderLocations = arrayOf(
    )
    private val triggers = arrayOf(
        Trigger(64.8) {
            val strikeLocation = thunderLocations.random()!!
            players.forEach {
                try {
                    it.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 100f, 1f)
                    it.strikeLightningEffect(strikeLocation)
                } catch (e: Exception) {
                }
            }
        }
    )

    private fun getTotalRidePathTime(): Long = config?.frames?.sumOf { it.duration.toInt() }?.toLong() ?: 0L

    fun getMillisLeft(): Long? {
        if (isRunning()) {
            val totalTime = (((BENCH_ANIMATION_TIME + 1L) * 2L) + getTotalRidePathTime()).toLong() + 10000 + 5000
            val left = totalTime - (System.currentTimeMillis() - getLastStartTime())
            return left
        }
        return null
    }

    init {
        for (entity in area.loc1.world!!.entities) {
            if (area.isInArea(entity.location) && entity.customName == rideName) {
                entity.remove()
            }
        }
        showController = ShowController()

        config = loadConfig()!!
        fireworkSpawns = loadFireworksConfig()

        cars.add(SoarinBench(145.5, 1.8))
        cars.add(SoarinBench(145.5, 4.5))

        cars.add(SoarinBench(134.5, 1.8))
        cars.add(SoarinBench(134.5, 4.5))

        stationSegment = SplinedTrackSegment("station", null)
        stationSegment.add(
        )
        stationSegment.length
        //        Logger.console("length " + stationSegment.getLength());

        updateCarts(true)

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            showController.update()
        }, 1, 1)
        rideDoor.forEach {
            it.block.open(true)
        }

        val noCheatPlus = Bukkit.getPluginManager().getPlugin("NoCheatPlus")
        if (noCheatPlus != null) {
            val listener = object : AbstractNCPHook() {
                override fun getHookName(): String = "Craftventure/Soarin"
                override fun getHookVersion(): String = "SNAPSHOT"

                override fun onCheckFailure(checkType: CheckType, player: Player, info: IViolationInfo): Boolean {
                    if (players.any { it.uniqueId == player.uniqueId }) {
                        return true
                    }
                    return super.onCheckFailure(checkType, player, info)
                }
            }

            NCPHookManager.addHook(
                arrayOf(
                    CheckType.ALL,
                ), listener
            )
        }
    }

    override fun stop() {
        stopFlying()

        super.stop()

        flying?.remove()
        flying = null

        lastPathIndex = 0
        railsOffset = 0.0

        AudioServerApi.disable("soarin_onride")
        rideDoor.forEach {
            it.block.open(true)
        }
//        ScriptManager.stop("soarin", "ride")
    }

    override fun prepareStart() {
        currentFlyRoute = flyRoute
        super.prepareStart()
        lastUpdateTime = 0

        val time = System.currentTimeMillis()
        AudioServerApi.enable("soarin_onride")
        AudioServerApi.sync("soarin_onride", time)

        for (soarinBench in cars) {
            soarinBench.syncMusic(time)
        }
        rideDoor.forEach {
            it.block.open(false)
        }
//        ScriptManager.start("soarin", "ride")
    }

    override fun onAutoScheduleStarted() {
        super.onAutoScheduleStarted()
        (TrackedRideManager.getTrackedRide("ccr")!!.getSegmentById("station") as StationSegment)
            .tryDispatchNow(StationSegment.DispatchRequestType.AUTO)
    }

    override fun onLeave(player: Player, vehicle: Entity?) {
        super.onLeave(player, vehicle)
        GameModeManager.setDefaults(player)
        AudioServerApi.remove("soarin_onride", player)
    }

    override fun provideRunnable(): FlatrideRunnable {
        state = State.STARTING
        frame = 0
        ticksInState = 0
        return object : FlatrideRunnable() {
            override fun updateTick() {
                update(startTime)
            }
        }
    }

    override fun removePlayer(player: Player, dismounted: Entity?) {
        super.removePlayer(player, dismounted)
        player.teleport(exitLocation)
    }

    private fun updateFlyRoute() {
        val flyRoute = mutableListOf<SoarinPathPart>()

        config?.frames?.forEach { frame ->
            val lines =
                File(CraftventureCore.getInstance().dataFolder, "data/ride/$rideName/${frame.path}.txt").readLines()
            val positions = mutableListOf<Position>()

            for (line in lines) {
                val parts = line.split("/")
                if (parts.size != 7) continue

                positions.add(
                    Position(
                        parts[0].toDouble(),
                        parts[1].toDouble(),
                        parts[2].toDouble(),
                        parts[3].toFloat(),
                        parts[4].toFloat()
                    )
                )
            }

            val linear = positions.size == 2

            val a: IPositionInterpolator = if (linear)
                LinearInterpolator.instance
            else
                CubicInterpolator.instance

            val b: IPolarCoordinatesInterpolator = if (linear)
                LinearInterpolator.instance
            else
                CubicInterpolator.instance

            val interpolator = Interpolator(positions.toTypedArray(), a, b)

            flyRoute.add(
                SoarinPathPart(
                    frame,
                    interpolator
                )
            )
        }

//        Logger.info("Loaded soarin with ${flyRoute.size} route parts")

        this.flyRoute = flyRoute.toTypedArray()
    }

    fun update(startTime: Long) {
//        Logger.info("Tick")
        frame++
        ticksInState++

        for (player in players.toTypedArray()) {
            if (player.spectatorTarget == null && !isLinkingPlayers && !isRespawningCamera) {
//                Logger.info("${player.name} is not spectating")
                removePlayer(player, null)
            }
        }

        if (frame > 2 || state == State.FLYING) {
            frame = 0
            val deltaTime = System.currentTimeMillis() - startTime

            val last = (lastUpdateTime / 1000.0)
            val now = (deltaTime / 1000.0)

//            Logger.info("Updating soarin with $last > $now")
//            Logger.debug(now.format(2), true)
            timedRideDispatchRequests.filter { it.atSeconds >= last && it.atSeconds < now }.map {
                it.execute()
            }
            triggers.filter { it.atSeconds >= last && it.atSeconds < now }.map {
                it.execute()
            }
            fireworkSpawns.filter { it.spawnTime >= last && it.spawnTime < now }.map {
                //                Logger.debug("Spawning fireworks for ${it.spawnTime.format(2)} ${it.atSeconds.format(2)} at $last > $now")
                it.execute()
            }
            playerTimeSpans.filter {
                val start = it.start
                val end = it.end ?: start
                if (end == it.start) return@filter start >= last && start < now

                start < now && end >= last
            }.map {
                //                Logger.info("Applying timespan ${now.format(2)}> ${it.start.format(2)}-${it.end?.format(2)}")
                it.execute(now)
            }

            lastUpdateTime = deltaTime

            if (state == State.STARTING) {
                val t = (ticksInState / (20 * BENCH_ANIMATION_TIME)).clamp(0.0, 1.0)
                railsOffset = t * (stationSegment.length - 6)
                if (t == 1.0) {
                    if (ticksInState > 20 * (BENCH_ANIMATION_TIME + 1.0)) {
                        ticksInState = 0
                        state = State.FLYING
                        startFlying(startTime + ((BENCH_ANIMATION_TIME + 1.0) * 1000.0).toLong())
                    }
                }
            } else if (state == State.FLYING) {
                val currentAnimationTime = System.currentTimeMillis() - flyingStartTime

                var hasUpdatedFrame = false
                var currentFrameStart = 0L
                for (i in 0 until currentFlyRoute.size) {
                    val path = currentFlyRoute[i]

                    if (currentFrameStart + path.duration > currentAnimationTime) {
                        val t = (currentAnimationTime - currentFrameStart) / (path.duration.toDouble())
                        path.interpolator.getPoint(cachePosition, t.clamp(0.0, 1.0))
                        val location = cachePosition.toLocation(cacheLocation)

                        if (i != lastPathIndex) {
                            lastPathIndex = i

                            currentFlyRoute[lastPathIndex].frame.apply {
                                val hour = this.startHour
                                val minute = this.startMinute
                                if (hour != null && minute != null) {
                                    val time = GameTimeUtils.hoursMinutesToTicks(hour, minute)
                                    players.forEach {
                                        PlayerTimeManager.setFrozenTime(it, time)
                                    }
                                }
                            }
                            respawnFlyingArmorstand(true)
                        }

//                        Logger.debug("Moving to yaw ${location.yaw.format(1)}")

                        EntityUtils.forceTeleport(flying!!, location)
//                        for (player in players)
//                            EntityUtils.forceTeleport(player, location)
//                        Logger.info("$i ${t.format(2)} ${cacheLocation.yaw.format(3)} ${cacheLocation.pitch.format(3)} ${cacheLocation.x.format(2)} ${cacheLocation.y.format(2)} ${cacheLocation.z.format(2)}")
//                        flying?.get
                        hasUpdatedFrame = true

                        sendUpdatePackets(location)
                        break
                    }

                    currentFrameStart += path.duration
                }

                if (!hasUpdatedFrame) {
                    ticksInState = 0
                    stopFlying()
                    state = State.STOPPING
                }
            } else if (state == State.STOPPING) {
                val t = 1 - (ticksInState / (20 * BENCH_ANIMATION_TIME)).clamp(0.0, 1.0)
                railsOffset = t * (stationSegment.length - 6)

                if (railsOffset <= 0 && ticksInState > (20 * (BENCH_ANIMATION_TIME + 1))) {
                    railsOffset = 0.0
                    updateCarts(true)
                    stop()
                    return
                }
            }

            if (deltaTime > TimeUnit.MINUTES.toMillis(3) + TimeUnit.SECONDS.toMillis(20)) {
                updateCarts(true)
                stop()
                return
            }

            updateCarts(false)
        }
    }

    private fun sendUpdatePackets(location: Location) {
        val packet = WrapperPlayServerEntityTeleport()
        packet.entityID = flying!!.entityId
        packet.x = location.x
        packet.y = location.y
        packet.z = location.z
        packet.yaw = location.yaw
        packet.pitch = location.pitch
        players.forEach {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(it, packet.handle, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val packet2 = WrapperPlayServerEntityHeadRotation()
        packet2.entityID = flying!!.entityId
        packet2.setHeadYaw(location.yaw.toDouble())
//                        packet2.yaw = location.yaw
//                        packet2.pitch = location.pitch
//                        packet2.onGround = flying!!.isOnGround
        players.forEach {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(it, packet2.handle, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun handlesExit(player: Player, dismounted: Entity, car: SoarinBench): Boolean {
        if (isRespawningCamera || isLinkingPlayers) {
//            Logger.info("Handling exit")
            return true
        }
//        Logger.info("Not handling exit")
        return if (state == State.FLYING) {
            true
        } else super.handlesExit(player, dismounted, car)
    }

    enum class State {
        STARTING, FLYING, STOPPING
    }

    override fun updateCarts(forceTeleport: Boolean) {
        for (soarinBench in cars) {
            stationSegment.getPosition(railsOffset + soarinBench.splineOffset, position)
            soarinBench.teleport(position.x, position.y - 4.2, position.z, 0f, 0f, forceTeleport)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerTeleport(playerTeleportEvent: PlayerTeleportEvent) {
        if (isRespawningCamera && playerTeleportEvent.player in players) {
            if (playerTeleportEvent.cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
                playerTeleportEvent.isCancelled = false
            }
        }
    }

    private fun respawnFlyingArmorstand(respawning: Boolean) {
        val oldFlying = flying
        area.world.entities.filter { it is Zombie && it !== oldFlying && it.customName == rideName }.map { it.remove() }
        isRespawningCamera = true
        cachePosition.toLocation(cacheLocation)
//        Logger.debug("Respawn to yaw ${cacheLocation.yaw.format(1)}")
        flying = cacheLocation.spawn()
//        Logger.info("Spawning camera at ${cacheLocation.x.format(2)} ${cacheLocation.y.format(2)} ${cacheLocation.z.format(2)}")
        flying?.let {
            it.customName = rideName
            it.setGravity(false)
            it.isSilent = true
            it.setAI(false)
            it.isInvulnerable = true
            it.isPersistent = false
            it.isInvisible = true
//            it.isVisible = false
            it.setInstantUpdate()
            it.setOwner(this)
        }
        if (respawning) {
            for (player in players) {
//                player.teleport(cacheLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)

                player.gameMode = GameMode.SPECTATOR
                player.spectatorTarget = flying
            }

//            players.forEachAllocationless { player ->
//                player.spawnParticle(Particle.CLOUD,
//                        cacheLocation.x, cacheLocation.y + 1.5, cacheLocation.z,
//                        120,
//                        2.0, 2.0, 2.0,
//                        0.0
//                )
//            }
        } else {
            isLinkingPlayers = true
            for (soarinBench in cars) {
                for (armorStand in soarinBench.entities) {
                    val player = armorStand.passengers.firstOrNull() as? Player
                    if (player != null) {
                        player.getOrCreateMetadata { SoarinMeta(player, armorStand) }
                        armorStand.eject()
                        EntityUtils.teleport(player, cacheLocation)
                        players.add(player)
                        EquipmentManager.reapply(player)

                        player.gameMode = GameMode.SPECTATOR
                        player.spectatorTarget = flying
                    }
                }
            }
            executeSync(1) {
                isLinkingPlayers = false
            }
        }
        oldFlying?.remove()
        sendUpdatePackets(cacheLocation)
        isRespawningCamera = false
    }

    fun startFlying(startTime: Long) {
        currentFlyRoute[0].interpolator.getPoint(cachePosition, 0.0).toLocation(cacheLocation)

        flyingStartTime = startTime
        currentSplineIndex = 0
        respawnFlyingArmorstand(false)
//        for (soarinBench in cars) {
//            for (armorStand in soarinBench.entities) {
//                val player = armorStand.passengers.firstOrNull() as? Player
//                if (player != null) {
//                    player.getOrCreateSoarinMeta(armorStand)
//                    armorStand.eject()
//                    player.teleport(cacheLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
//                    players.add(player)
//                    WornItemManager.update(player)
//
//                    player.gameMode = GameMode.SPECTATOR
//                    player.spectatorTarget = flying
//                }
//            }
//        }
    }

    fun stopFlying() {
        flying?.remove()
        flying = null

        val cachedPlayers = ArrayList(players)

        for (player in players) {
            GameModeManager.setDefaults(player)

            player.getMetadata<SoarinMeta>()?.apply {
                player.teleport(seat)
                seat.addPassenger(player)
            }
        }

        players.clear()

        cachedPlayers.forEach { EquipmentManager.reapply(it) }
    }

    @EventHandler
    fun onPlayerWornItemsChanged(event: PlayerEquippedItemsUpdateEvent) {
        players.firstOrNull {
            it.player === event.player
        }?.let {
            event.appliedEquippedItems.clearAll()
            event.appliedEquippedItems.helmetItem = ItemStack(Material.CARVED_PUMPKIN).toEquippedItemData()
        }
    }

    override fun postLeave(player: Player) {
        super.postLeave(player)
        players.remove(player)
        player.removeMetadata<SoarinMeta>()
        EquipmentManager.reapply(player)
        PlayerTimeManager.reset(player)
    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    fun onFlyingChunkUnload(event: ChunkUnloadEvent) {
//        flying?.let { flying ->
//            val chunk = flying.location.chunk
//            if (event.chunk.x == chunk.x && event.chunk.z == chunk.z) {
//                event.isCancelled = true
//            }
//        }
//    }

    inner class SoarinBench(
        private val x: Double,
        val splineOffset: Double
    ) : FlatrideCar<ArmorStand>(arrayOfNulls(6)) {

        fun syncMusic(time: Long) {
            for (entity in entities) {
                val player = entity.passengers.firstOrNull() as? Player
                if (player != null)
                    AudioServerApi.addAndSync("soarin_onride", player, time)
            }
        }

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            for (i in entities.indices) {
                if (entities[i] == null || !entities[i].isValid) {
                    entities[i] = area.loc1.world!!.spawn(
                        Location(
                            area.loc1.world,
                            this.x - i,
                            y,
                            z,
                            0f,
                            0f
                        ), ArmorStand::class.java
                    )
                    entities[i]!!.isPersistent = false
                    entities[i].setOwner(this@Soarin)
                    entities[i].setGravity(false)
                    entities[i].setBasePlate(false)
                    entities[i].isVisible = false
                    entities[i].customName = rideName
                    entities[i].addDisabledSlots(*EquipmentSlot.values())
                    if (i == 2)
                        entities[i].setHelmet(MaterialConfig.SOARIN)
                } else {//if (entities[i].getPassenger() != null || forceTeleport) {
                    EntityUtils.teleport(
                        entities[i],
                        this.x - i,
                        y,
                        z,
                        0f,
                        0f
                    )
                    //                    entities[i].setHeadPose(new EulerAngle((left ? -pitch : pitch) + Math.PI, 0, 0));
                }

                if (forceModelUpdate) {
                    if (i == 2)
                        entities[i].setHelmet(MaterialConfig.SOARIN)
                }
            }
        }
    }

    fun reloadRoute() {
        try {
            config = loadConfig()!!
        } catch (e: Exception) {
            e.printStackTrace()
        }
        fireworkSpawns = loadFireworksConfig()
    }

    private fun loadFireworksConfig(): Array<FireworkTrigger> {
        try {
            val config = YamlConfiguration.loadConfiguration(
                File(
                    CraftventureCore.getInstance().dataFolder,
                    "data/ride/$rideName/fireworks.yml"
                )
            )
            val triggers = mutableListOf<FireworkTrigger>()
            val fireworks = HashMap<String, ItemStack>()
            for (fireworkName in config.getConfigurationSection("fireworks")!!.getKeys(false)) {
                val firework =
                    ItemStack.deserializeBytes(config.getString("fireworks.$fireworkName")!!.decodeBase64ToByteArray())
                fireworks[fireworkName] = firework
            }

            val items = config.getMapList("items")
            for (itemData in items) {
                val rawLocation = itemData["location"] as Map<String, Any>
                val rawVelocity = itemData["velocity"] as Map<String, Any>?
                val trigger = FireworkTrigger(
                    itemData["atSeconds"] as Double,
                    itemData["atDefinesExplode"] as Boolean,
                    itemData["lifeTimeSeconds"] as Double,
                    fireworks[itemData["firework"] as String]!!,
                    Vector(
                        (rawLocation["x"].toString().toDouble()),
                        (rawLocation["y"].toString().toDouble()),
                        (rawLocation["z"].toString().toDouble()),
                    ),
                    Vector(
                        (rawVelocity?.get("x")?.toString())?.toDoubleOrNull() ?: 0.0,
                        (rawVelocity?.get("y")?.toString())?.toDoubleOrNull() ?: 0.0,
                        (rawVelocity?.get("z")?.toString())?.toDoubleOrNull() ?: 0.0,
                    )
                )
//                Logger.debug("Added firework trigger at atSeconds=${trigger.atSeconds} lifeTimeSeconds=${trigger.lifeTimeSeconds} spawnAt=${trigger.spawnTime}")
                triggers.add(trigger)
            }
//            Logger.debug("Loaded ${triggers.size} fireworks")
//            if (CraftventureCore.isTestServer()) {
            val crewList = Bukkit.getOnlinePlayers().filter { it.isCrew() }
            for (trigger in triggers) {
                trigger.execute(crewList)
            }
//            }

            return triggers.toTypedArray()
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.warn("Failed to load Soarin fireworks: ${e.message}", true)
        }
        return emptyArray()
    }

    private fun loadConfig(): SoarinConfig? {
        val file = File(CraftventureCore.getInstance().dataFolder, "data/ride/$rideName/config.json")
        if (file.exists()) {
            try {
                return CvMoshi.adapter(SoarinConfig::class.java).fromJson(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.warn("Failed to load Soarin route: ${e.message}", true)
            }
        }
        return null
    }


    protected inner class ShowController {
        private var state = ShowState.IDLE
        private var inStateSince = System.currentTimeMillis()
        private var gatheringPlayerStartingTime: Long = 0

        init {
            enterDoors.forEach {
                it.block.open(true)
            }
            exitDoors.forEach {
                it.block.open(false)
            }
        }

        fun setState(newState: ShowState) {
            if (this.state != newState) {
//                Logger.info("Preshow state=$newState (was $state)")
                this.state = newState
                if (this.state == ShowState.IDLE || this.state == ShowState.WAITING_FOR_DOORS || this.state == ShowState.WAITING_FOR_RIDE_START) {
                    stop()
                }
                inStateSince = System.currentTimeMillis()

                enterDoors.forEach {
                    it.block.open(state == ShowState.IDLE)
                }
                exitDoors.forEach {
                    it.block.open(state == ShowState.WAITING_FOR_RIDE_START)
                }

                when (state) {
                    ShowState.PLAYING -> start()
                    ShowState.WAITING_FOR_DOORS -> {
                        Bukkit.getOnlinePlayers().filter { preshowArea.isInArea(it) }.map {
                            it.sendTitleWithTicks(
                                stay = 20 * 3,
                                subtitle = CVTextColor.serverNotice + "Please wait a moment until you can start boarding..."
                            )
                        }
                    }

                    ShowState.WAITING_FOR_RIDE_START -> {
                        Bukkit.getOnlinePlayers().filter { preshowArea.isInArea(it) }.map {
                            it.sendTitleWithTicks(
                                stay = 20 * 3,
                                subtitle = CVTextColor.serverNotice + "Please continue to the boarding area"
                            )
                        }
                    }

                    else -> {}
                }
                if (state == ShowState.WAITING_FOR_DOORS && this@Soarin.isRunning()) {
                    for (player in Bukkit.getOnlinePlayers()) {
                        if (preshowArea.isInArea(player)) {
//                            player.sendMessage(Translation.RIDE_GONBAO_PRESHOW_WAITING_FOR_RIDE.getTranslation(player))
                            player.sendMessage(CVTextColor.serverNotice + "Please wait for Henry Eulenstein to finish his preparations")
                        }
                    }
                }
            }
        }

        fun update() {
            if (state == ShowState.IDLE) {
                var containsPlayers = false
                for (player in Bukkit.getOnlinePlayers()) {
                    val inArea = preshowArea.isInArea(player)
                    if (inArea) {
                        display(
                            player,
                            Message(
                                id = ChatUtils.ID_RIDE,
                                text = Component.text(
                                    "Please wait for Henry Eulenstein to prepare his briefing",
                                    CVTextColor.serverNotice
                                ),
                                type = MessageBarManager.Type.RIDE,
                                untilMillis = TimeUtils.secondsFromNow(1.0),
                            ),
                            replace = true,
                        )
                        containsPlayers = true
                    }
                }
                if (containsPlayers) {
                    if (gatheringPlayerStartingTime == -1L) {
                        gatheringPlayerStartingTime = System.currentTimeMillis()
                    }
                } else {
                    gatheringPlayerStartingTime = -1
                }
                if (gatheringPlayerStartingTime > 0) {
                    if (gatheringPlayerStartingTime < System.currentTimeMillis() - 15000) {
                        gatheringPlayerStartingTime = -1
                        //                        Logger.console("Starting gonbao because a player was inside the area for 5 secs");
                        setState(ShowState.PLAYING)
                    }
                }
            } else if (state == ShowState.PLAYING) {
                if (inStateSince < System.currentTimeMillis() - 64000) {
                    setState(ShowState.WAITING_FOR_DOORS)
                }
            } else if (state == ShowState.WAITING_FOR_DOORS) {
                if (!this@Soarin.isRunning()) {
                    setState(ShowState.WAITING_FOR_RIDE_START)
                } else {
                    val waitingMillisLeft = getMillisLeft()
//                    Logger.debug("waitingMillisLeft=$waitingMillisLeft")
                    for (player in Bukkit.getOnlinePlayers()) {
                        val inArea = preshowArea.isInArea(player)
                        if (inArea) {
                            display(
                                player,
                                Message(
                                    id = ChatUtils.ID_RIDE,
                                    text = Component.text(
                                        "Attention: Boarding is delayed, please wait ${
                                            DateUtils.format(
                                                waitingMillisLeft
                                                    ?: -1L, "?"
                                            )
                                        }", CVTextColor.serverNotice
                                    ),
                                    type = MessageBarManager.Type.RIDE,
                                    untilMillis = TimeUtils.secondsFromNow(1.0),
                                ),
                                replace = true,
                            )
                        }
                    }
                }
            } else if (state == ShowState.WAITING_FOR_RIDE_START) {
                if (this@Soarin.isRunning() || inStateSince < System.currentTimeMillis() - 20000) {
                    //                    Logger.console("GonBao preshow resetting, has ride started? " + GonBaoCastle.this.isRunning());
                    setState(ShowState.IDLE)
                }
            }
        }

        private fun start() {
            ScriptManager.stop("soarin", "preshow")
            ScriptManager.start("soarin", "preshow")
            AudioServerApi.enable("soarin_preshow")
            AudioServerApi.sync("soarin_preshow", System.currentTimeMillis())
        }

        private fun stop() {
//            ScriptManager.stop("soarin", "preshow")
            AudioServerApi.disable("soarin_preshow")
        }
    }

    enum class ShowState {
        IDLE, PLAYING, WAITING_FOR_DOORS, WAITING_FOR_RIDE_START
    }

    private class SoarinPathPart(
        val frame: SoarinFrame,
        val interpolator: Interpolator
    ) {
        val duration: Long
            get() = frame.duration
    }

    @JsonClass(generateAdapter = true)
    class SoarinConfig(
        val frames: Array<SoarinFrame>
    )

    @JsonClass(generateAdapter = true)
    class SoarinFrame(
        @Json(name = "start_hour")
        val startHour: Int? = null,
        @Json(name = "start_minute")
        val startMinute: Int? = null,
        val duration: Long,
        val path: String
    )

    private inner class PlayerTimeControlSpan(
        val start: Double,
        val end: Double? = null,
        val timeProvider: (at: Double, span: PlayerTimeControlSpan) -> Long
    ) {
        fun execute(at: Double) {
            val time = timeProvider(at, this)
//            Logger.info("Updating time to $time")
            players.forEach {
                PlayerTimeManager.setFrozenTime(it, time)
            }
        }
    }

    private inner class Trigger(
        val atSeconds: Double,
        val action: () -> Unit
    ) {
        fun execute() {
            action()
        }
    }

    private inner class FireworkTrigger(
        val atSeconds: Double,
        val atDefinesExplode: Boolean,
        val lifeTimeSeconds: Double,
        val firework: ItemStack,
        val location: Vector,
        val velocity: Vector
    ) {
        val spawnTime = if (atDefinesExplode) atSeconds - lifeTimeSeconds else atSeconds
        fun execute() {
            execute(players.toList())
        }

        fun execute(players: List<Player>) {
            FireworkUtils.spawn(
                exitLocation.world!!,
                location.x,
                location.y,
                location.z,
                firework,
                velocity.x,
                velocity.y,
                velocity.z,
                lifeTimeSeconds,
                players
            )
//            Logger.debug("Triggering firework explode at=${atSeconds.format(2)}")
        }
    }

    private class TimedRideDispatchRequest(
        val atSeconds: Double,
        val ride: String,
        val stationName: String? = null
    ) {
        fun execute() {
//            Logger.info("Executing dispatch request for $ride at $atSeconds with station=$stationName")
            val ride = TrackedRideManager.getTrackedRide(ride) ?: FlatrideManager.getFlatride(ride)
            if (ride is OperableCoasterTrackedRide) {
                val segment =
                    if (stationName != null) ride.getSegmentById(stationName) else ride.trackSegments.firstOrNull { it is StationSegment }
                if (segment is StationSegment) {
                    if (!segment.tryDispatchNow(StationSegment.DispatchRequestType.AUTO)) {
//                        segment.tryDispatchNow(StationSegment.DispatchRequestType.OPERATOR)
                    }
                } else {
                    Logger.warn("Couldn't find StationSegment for Soarin' trigger at=${atSeconds.format(2)} ride=$ride stationName=$stationName")
                }
            } else if (ride is Flatride<*>) {
                if (ride.canStart()) {
                    ride.start()
                }
            } else {
                Logger.warn("Couldn't find ride=$ride for Soarin'")
            }
        }
    }

    private class SoarinMeta(
        player: Player,
        val seat: Entity
    ) : BasePlayerMetadata(player) {
        override fun debugComponent() = Component.text("seat=${seat.entityId}")
    }

    companion object {
        private const val BENCH_ANIMATION_TIME = 5.5
        private var _instance: Soarin? = null

        fun getInstance(): Soarin {
            if (_instance == null) {
                _instance = Soarin()
            }
            return _instance!!
        }
    }
}
