package net.craftventure.core.ride.queue

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.manager.BossBarManager
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.isAfk
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.Allow
import net.craftventure.core.manager.Deny
import net.craftventure.core.manager.GrantResult
import net.craftventure.core.manager.PlayerStateManager.allowStateManagement
import net.craftventure.core.manager.PlayerStateManager.allowTeleporting
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.manager.PlayerStateManager.isAllowedToJoinRideQueue
import net.craftventure.core.metadata.GenericPlayerMeta
import net.craftventure.core.ride.RideInstance
import net.craftventure.core.ride.trackedride.TrackedRide
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.scene.TracklessStationScene
import net.craftventure.core.serverevent.ProvideLeaveInfoEvent
import net.craftventure.core.utils.ItemStackUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

class RideQueue @JvmOverloads constructor(
    val id: String = "main",
    val ride: RideInstance,
    val joinArea: Area,
    val passengerCountPerTrain: Int,
    val averageSecondsBetweenDepartures: Double,
    val boardingDelegate: BoardingDelegate,
    val activeThresshold: Int = passengerCountPerTrain + 1,
) : Listener {
    private val bossBarId = "${ride.id}/queue/$id"
    private var listeners = setOf<QueueListener>()

    fun addListener(listener: QueueListener) {
        listeners = listeners + listener
//        Logger.debug("addListener ${listeners.size}")
    }

    fun removeListener(listener: QueueListener) {
        listeners = listeners - listener
//        Logger.debug("removeListener ${listeners.size}")
    }

    private var lastPut = 0L
    private var started = false
    private var task = 0
    var isActive = false
        private set(value) {
            if (field != value) {
                field = value
//                Logger.info("Queue for ${getRideName()} active=$active")
                if (value) {
                    boardingDelegate.setQueueActive(this, true)
                    queue.forEach {
                        sendJoinMessage(it.player)
                    }
                } else {
                    boardingDelegate.setQueueActive(this, false)
                    queue.forEach {
                        sendQueueCancelMessage(it.player)
                    }
                }
                listeners.forEach { it.onActiveChanged(this) }
            }
        }

    private var queue = ArrayDeque<QueueEntry>()
    private var updatingPlayer: Player? = null

//    init {
//        for (i in 0..100) {
//            val departureIndex = departureFromQueueIndex(i)
//            Logger.info("$i departure=$departureIndex seconds=${getWaitingTimeForDeparture(departureIndex)}-${getWaitingTimeForDeparture(departureIndex + 1)}")
//        }
//    }

    private fun update() {
        synchronized(queue) {
            removeInvalid()
            isActive = if (isActive) {
                queue.isNotEmpty()
            } else {
                queue/*.size*/.count { it.getPutGrant().isAllowed() } >= activeThresshold || activeThresshold == 0
            }
            queue.forEach {
                val allowed = it.getPutGrant().isAllowed()
                if (it.timeSinceSkipped != null && allowed) {
                    it.timeSinceSkipped = null
                } else if (it.timeSinceSkipped == null && !allowed) {
                    it.timeSinceSkipped = System.currentTimeMillis()
                }
            }
            if (isActive) {
//                Logger.info("Updating queue with ${queue.size} players")
                if (boardingDelegate.canPut()) {
                    var success: Boolean
                    do {
                        success = false
                        val player = queue.firstOrNull { it.getPutGrant().isAllowed() }
                        if (player != null) {
                            updatingPlayer = player.player
                            try {
                                logcat { "Putting ${player.player.name} into ${ride.displayName()} for queue $id" }
//                                    Logger.info("Putting ${player.player.name}")
                                success = boardingDelegate.put(player.player)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (success) {
                                lastPut = System.currentTimeMillis()
                                player.player.getMetadata<GenericPlayerMeta>()?.blockManualVehicleExiting()
                                TitleManager.remove(player.player, TitleManager.Type.RideQueue)
//                                    Logger.info("Success ${player.player.name} ${player.player.isInsideVehicle}")
                                if (queue.removeAll { it.player == player.player }) {
                                    player.player.gameState()?.rideQueue = null
                                }
                            }
                            updatingPlayer = null
                        }
                    } while (success)
                }

                var puttableIndex = 0
                queue.forEachIndexed { index, queueHolder ->
                    val putGrant = queueHolder.getPutGrant()
                    val canBePut = putGrant.isAllowed()
                    if (canBePut || !queueHolder.isConsideredSkipped) puttableIndex++
                    val operated = boardingDelegate.isBeingOperated()
                    val lowerBounds = getLowerBoundsEstimation(departureFromQueueIndex(puttableIndex - 1))
                    val upperBounds = getUpperBoundsEstimation(departureFromQueueIndex(index))
                    if (!canBePut) {
                        BossBarManager.display(
                            queueHolder.player,
                            BossBarManager.Message(
                                bossBarId,
                                arrayOf(
                                    if (lowerBounds != null && upperBounds != null)
                                        Component
                                            .text(
                                                "#${index + 1} in queue at ${
                                                    DateUtils.format(lowerBounds * 1000L, "01s")
                                                }-${DateUtils.format(upperBounds * 1000L, "01s")}.",
                                                CVTextColor.serverNotice
                                            )
                                            .append(CVTextColor.serverNoticeAccent + " /leave")
                                            .append(CVTextColor.serverNotice + " to leave the queue")
                                    else
                                        Component
                                            .text(
                                                "#${index + 1} in the queue. Use",
                                                CVTextColor.serverNotice
                                            )
                                            .append(CVTextColor.serverNoticeAccent + " /leave")
                                            .append(CVTextColor.serverNotice + " to leave the queue"),
                                    if (putGrant is Deny)
                                        CVTextColor.serverNotice + ("Can't board: " + putGrant.reason)
                                    else
                                        CVTextColor.serverNotice + "Can't board",
                                ),
                                priority = BossBarManager.Priority.queue,
                                untilMillis = TimeUtils.secondsFromNow(3.0)
                            ),
                            replace = true,
                        )
                        return@forEachIndexed
                    } else if (operated || lowerBounds == null || upperBounds == null) {
                        BossBarManager.display(
                            queueHolder.player,
                            BossBarManager.Message(
                                bossBarId,
                                arrayOf(
                                    Component
                                        .text(
                                            "#${index + 1} in the queue. Estimation unavailable. Use",
                                            CVTextColor.serverNotice
                                        )
                                        .append(CVTextColor.serverNoticeAccent + " /leave")
                                        .append(CVTextColor.serverNotice + " to leave the queue"),
                                ),
                                priority = BossBarManager.Priority.queue,
                                untilMillis = TimeUtils.secondsFromNow(3.0)
                            ),
                            replace = true,
                        )
                        return@forEachIndexed
                    }
                    BossBarManager.display(
                        queueHolder.player,
                        BossBarManager.Message(
                            bossBarId,
                            arrayOf(
                                Component
                                    .text(
                                        "#${index + 1} in queue at ${
                                            DateUtils.format(lowerBounds * 1000L, "00s")
                                        }-${DateUtils.format(upperBounds * 1000L, "00s")}.",
                                        CVTextColor.serverNotice
                                    )
                                    .append(CVTextColor.serverNoticeAccent + " /leave")
                                    .append(CVTextColor.serverNotice + " to leave the queue"),
                            ),
                            priority = BossBarManager.Priority.queue,
                            untilMillis = TimeUtils.secondsFromNow(3.0),
                        ),
                        replace = true,
                    )
                }
            }
        }
    }

    fun start() {
        if (started) return
        Bukkit.getServer().pluginManager.registerEvents(this, CraftventureCore.getInstance())
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this::update, 20, 20)
        started = true
    }

    fun stop() {
        if (!started) return
        queue.iterator().forEach { remove(it.player) }
        Bukkit.getScheduler().cancelTask(task)
        HandlerList.unregisterAll(this)
        started = false
    }

    private fun departureFromQueueIndex(index: Int) = Math.ceil(index / passengerCountPerTrain.toDouble()).toInt()

//    private fun calculateNextDepartureTime() = station.estimatedMillisecondsUntil?.let { (it / 1000).toInt() }
//            ?: averageSecondsBetweenDepartures

    private fun getLowerBoundsEstimation(index: Int): Int? {
        if (!boardingDelegate.isQueueTimeAvailable()) return null
        return getWaitingTimeForDeparture(departureFromQueueIndex(index))
    }

    private fun getUpperBoundsEstimation(index: Int): Int? {
        if (!boardingDelegate.isQueueTimeAvailable()) return null
        return getWaitingTimeForDeparture(departureFromQueueIndex(index) + 1)
    }

    fun getQueuedCount() = queue.size

    fun getCurrentEstimateMin() = getLowerBoundsEstimation(departureFromQueueIndex(queue.size))
    fun getCurrentEstimateMax() = getUpperBoundsEstimation(departureFromQueueIndex(queue.size))

    private fun getWaitingTimeForDeparture(index: Int): Int? {
        val lastDispatch = boardingDelegate.getLastDispatch()
        // TODO: Don't use expected when being operated?
        val expectedNextDispatchIn = if (lastDispatch != null && lastDispatch > lastPut) {
            lastDispatch + (averageSecondsBetweenDepartures * 1000) - System.currentTimeMillis()
        } else null
//        logcat { "expectedNextDispatchIn=$expectedNextDispatchIn lastDispatch=$lastDispatch expect=${(lastDispatch ?: 0) + (averageSecondsBetweenDepartures * 1000)} now=${System.currentTimeMillis()}" }
        if (expectedNextDispatchIn != null) {
            val correction = max(averageSecondsBetweenDepartures - ceil(expectedNextDispatchIn / 1000.0), 0.0)
            return max((averageSecondsBetweenDepartures * index) - correction, 1.0).toInt()
        }
        return max((averageSecondsBetweenDepartures * index).toInt(), 1)
    }

    private fun getRideName() = boardingDelegate.getRideName()

    private fun sendQueueCancelMessage(player: Player) {
//        Logger.info("Queue ${player.player.name}")
        player.displayTitle(
            TitleManager.TitleData.ofTicks(
                id = "ridequeue",
                type = TitleManager.Type.RideQueue,
                title = null,
                subtitle = Component.text("Queue for ${getRideName()} cancelled, enter at will", NamedTextColor.YELLOW),
                stayTicks = 20 * 3,
                fadeInTicks = 10,
                fadeOutTicks = 10,
            ),
            replace = true,
        )
    }

    private fun sendJoinMessage(player: Player) {
//        Logger.info("Send join ${player.player?.name}")
        player.displayTitle(
            TitleManager.TitleData.ofTicks(
                id = "ridequeue",
                type = TitleManager.Type.RideQueue,
                title = null,
                subtitle = Component.text("Queue for ${getRideName()} joined", NamedTextColor.YELLOW),
                stayTicks = 20 * 3,
                fadeInTicks = 10,
                fadeOutTicks = 10,
            ),
            replace = true,
        )
    }

    private fun sendLeave(player: Player) {
//        Logger.info("Send leave ${player.player?.name}")
        player.displayTitle(
            TitleManager.TitleData.ofTicks(
                id = "ridequeue",
                type = TitleManager.Type.RideQueue,
                title = null,
                subtitle = Component.text("Queue for ${getRideName()} left", NamedTextColor.RED),
                stayTicks = 20 * 3,
                fadeInTicks = 10,
                fadeOutTicks = 10,
            ),
            replace = true,
        )
    }

    private fun onJoined(player: QueueEntry) {
        logcat { "Queue $id for ${ride.displayName()} joined ${player.player.name}" }
        if (isActive) sendJoinMessage(player.player)
        listeners.forEach { it.onSizeChanged(this) }
        player.player.gameState()?.rideQueue = this
        TitleManager.remove(player.player, TitleManager.Type.RideQueue)
    }

    private fun onLeft(player: QueueEntry) {
        logcat { "Queue $id for ${ride.displayName()} left ${player.player.name}" }
        if (isActive) sendLeave(player.player)
        BossBarManager.remove(player.player, id = bossBarId)
        listeners.forEach { it.onSizeChanged(this) }
        player.player.gameState()?.rideQueue = null
    }

    private fun update(player: Player, location: Location, canCancel: Boolean = false): Boolean {
        if (player === updatingPlayer) return false
        synchronized(queue) {
            val queueHolder = queue.firstOrNull { it.player === player }
            val wasInArea = queueHolder != null
            val nowInArea = joinArea.isInArea(location) &&
                    player.isConnected() && !player.isAfk() &&
                    !player.isInsideVehicle && boardingDelegate.canEnterRide(player)

            if (wasInArea != nowInArea) {
//                if (!nowInArea)
//                    Logger.debug("canCancel=$canCancel")
//                if (active && !nowInArea && canCancel && queueHolder?.tryExit() != true) {
////                    Logger.debug(
////                        "wasInArea=$wasInArea wander=${wanderArea.isInArea(location)} join=${joinArea.isInArea(
////                            location
////                        )} ${location.toVector().asString()} $wanderArea $joinArea"
////                    )
//                    player.sendTitleWithTicks(0, 20 * 5, 20, errorTitle, errorNotice)
//                    player.displayNoFurther()
//                    return true
//                }
//                Logger.info("${player.name} inArea=$inArea nowInArea=$nowInArea")
                if (nowInArea && player.isAllowedToJoinRideQueue()
                        .isAllowed() && player.gameMode != GameMode.SPECTATOR
                ) {
                    QueueEntry(player).apply {
                        queue.add(this)
                        onJoined(this)
                    }
                } else if (wasInArea && !isActive) {
                    remove(player)
                }
            }
        }
        return false
    }

    private fun removeInvalid() {
        queue.removeAll { queueHolder ->
            (!queueHolder.isValid()).also { removed ->
                if (removed)
                    onLeft(queueHolder)
            }
        }
    }

    private fun remove(player: Player) {
        queue.removeAll { queueHolder ->
            (queueHolder.player === player).also { removed ->
                if (removed)
                    onLeft(queueHolder)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onRequestLeaveCommand(event: ProvideLeaveInfoEvent) {
        val queueHolder = queue.firstOrNull { it.player === event.player } ?: return
        if (event.player in joinArea) {
            return
        }

        event.data.add(
            ProvideLeaveInfoEvent.Entry(
                ProvideLeaveInfoEvent.Category.RideQueue,
                "Leave queue for ride ${ride.displayName()}",
                representation = ride.ride?.itemStackDataId?.let { ItemStackUtils.fromString(it) }
                    ?: ItemStack(Material.BARRIER),
            ) {
                remove(event.player)
                true
            })
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerMoveMonitor(event: PlayerLocationChangedEvent) {
        if (event.locationChanged)
            if (update(player = event.player, location = event.to, canCancel = !event.isTeleport)) {
                event.isCancelled = true
            }
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        logout(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        logout(event.player)
    }

    private fun logout(player: Player) {
        remove(player)
    }

    data class QueueEntry(
        val player: Player,
        val time: Long = System.currentTimeMillis()
    ) {
        private var sessionStart = System.currentTimeMillis()
        var lastExitTry = System.currentTimeMillis()
        var timeSinceSkipped: Long? = null

        val isConsideredSkipped get() = timeSinceSkipped?.let { it < System.currentTimeMillis() - 15000 } ?: false

        private var canPutTimeout = 0

//        val trySucceedIn: Long?
//            get() {
//                return if (deltaLast < DELTA_LAST_THRESHOLD) {
//                    if (sessionStart < now - TRY_TIME) {
//                        return true
//                    }
//                } else {
//                    TRY_TIME
//                }
//            }

        fun getPutGrant(): GrantResult {
            if (player.isAfk()) return Deny("Can't board while AFK")

            val gameState = player.gameState()

            val state = player.allowStateManagement(gameState)
            if (state.isNotAllowed()) {
                canPutTimeout = Bukkit.getServer().currentTick
                return state
            }

            val teleport = player.allowTeleporting(gameState)
            if (teleport.isNotAllowed()) {
                canPutTimeout = Bukkit.getServer().currentTick
                return teleport
            }

//            val vehicle = player.allowVehicleChange(gameState)
//            if (vehicle.isNotAllowed()) {
//                canPutTimeout = Bukkit.getServer().currentTick
//                return vehicle
//            }

            if (player.isInsideVehicle) {
                canPutTimeout = Bukkit.getServer().currentTick
                return Deny("Not allowed while inside of a vehicle")
            }

            if (player.isDead) {
                canPutTimeout = Bukkit.getServer().currentTick
                return Deny("Not allowed while dead")
            }

            if (!player.isValid) {
                canPutTimeout = Bukkit.getServer().currentTick
                return Deny("Not allowed while invalid")
            }

            if (player.isSneaking) {
                canPutTimeout = Bukkit.getServer().currentTick
                return Deny("Not allowed to enter ride while sneaking")
            }

            if (canPutTimeout < Bukkit.getServer().currentTick - 5)
                return Allow
            return Deny("Waiting for boarding timeout to expire")
        }

        fun tryExit(): Boolean {
            val now = System.currentTimeMillis()
            val deltaLast = now - lastExitTry
            lastExitTry = now

            if (deltaLast < DELTA_LAST_THRESHOLD) {
                if (sessionStart < now - TRY_TIME) {
                    return true
                }
            } else {
                sessionStart = now
            }

            return false
        }

        fun isValid() = player.isConnected() && player.gameMode != GameMode.SPECTATOR

        companion object {
            const val DELTA_LAST_THRESHOLD = 500
            const val TRY_TIME = 1000
        }
    }

    interface QueueListener {
        fun onSizeChanged(queue: RideQueue)
        fun onActiveChanged(queue: RideQueue)
    }

    interface BoardingDelegate {
        fun getRideName(): String
        fun isBeingOperated(): Boolean
        fun isQueueTimeAvailable(): Boolean
        fun canPut(): Boolean
        fun put(passenger: Player): Boolean
        fun setQueueActive(rideQueue: RideQueue, active: Boolean)
        fun canEnterRide(player: Player): Boolean
        fun getLastDispatch(): Long?

        fun isCompatibleWith(other: BoardingDelegate): Boolean = false
        val ride: RideInstance
    }

    abstract class Json {
        var id: String = "main"
        lateinit var joinArea: Area.Json
        var passengerCountPerTrain: Int = 0
        var averageSecondsBetweenDepartures: Double = 0.0
        var activeThresshold: Int = 0
    }

    interface TrackedRideBoardingDelegate : BoardingDelegate {
        abstract class Json {
            abstract fun create(trackedRide: TrackedRide): BoardingDelegate
        }
    }

    @JsonClass(generateAdapter = true)
    class TrackedRideJson : Json() {
        lateinit var boardingDelegate: TrackedRideBoardingDelegate.Json

        fun create(ride: TrackedRide) = RideQueue(
            id = id,
            ride = ride,
            joinArea = joinArea.create(),
            passengerCountPerTrain = passengerCountPerTrain,
            averageSecondsBetweenDepartures = averageSecondsBetweenDepartures,
            boardingDelegate = boardingDelegate.create(ride),
            activeThresshold = activeThresshold,
        )
    }

    interface TracklesssRideBoardingDelegate : BoardingDelegate {
        abstract class Json {
            abstract fun create(tracklessRide: TracklessRide): BoardingDelegate
        }
    }

    @JsonClass(generateAdapter = true)
    class TracklessRideJson : Json() {
        lateinit var boardingDelegate: TracklesssRideBoardingDelegate.Json

        fun create(ride: TracklessRide) = RideQueue(
            id = id,
            ride = ride,
            joinArea = joinArea.create(),
            passengerCountPerTrain = passengerCountPerTrain,
            averageSecondsBetweenDepartures = averageSecondsBetweenDepartures,
            boardingDelegate = boardingDelegate.create(ride),
            activeThresshold = activeThresshold,
        )
    }

    class RideStationBoardingDelegate(val station: StationSegment) : TrackedRideBoardingDelegate {
        override val ride
            get() = station.trackedRide

        override fun getRideName(): String =
            station.trackedRide.let { it.ride?.displayName ?: it.name } ?: "Unknown"

        override fun isBeingOperated(): Boolean = station.trackedRide.isBeingOperated
        override fun isQueueTimeAvailable(): Boolean = !station.trackedRide.isBeingOperated
        override fun canPut(): Boolean =
            station.state == StationSegment.StationState.HOLDING && station.targetTrain != null

        override fun put(passenger: Player): Boolean = station.targetTrain?.putPassenger(passenger) ?: false
        override fun setQueueActive(rideQueue: RideQueue, active: Boolean) {
            if (active)
                station.queue = rideQueue
            else
                station.queue = null
        }

        override fun canEnterRide(player: Player): Boolean =
            station.trackedRide.canEnter(player).isAllowed() || player.isCrew()

        override fun getLastDispatch(): Long? = station.getLastDispatchTime()

        @JsonClass(generateAdapter = true)
        class Json(
            val segmentId: String
        ) : TrackedRideBoardingDelegate.Json() {
            override fun create(trackedRide: TrackedRide) =
                RideStationBoardingDelegate(trackedRide.getSegmentById(segmentId) as StationSegment)
        }
    }

    class StationSceneBoardingDelegate(val station: TracklessStationScene) : TracklesssRideBoardingDelegate {
        override val ride
            get() = station.tracklessRide

        override fun getRideName(): String = ride.ride?.displayName ?: ride.id
        override fun isBeingOperated(): Boolean = ride.isBeingOperated
        override fun isQueueTimeAvailable(): Boolean = !ride.isBeingOperated
        override fun canPut(): Boolean =
            station.state == TracklessStationScene.State.HOLDING && station.currentGroup != null

        override fun put(passenger: Player): Boolean =
            station.currentGroup?.cars?.any { it.canEnter && it.putPassenger(passenger) } ?: false

        override fun setQueueActive(rideQueue: RideQueue, active: Boolean) {
            if (active)
                station.guestQueue = rideQueue
            else
                station.guestQueue = null
        }

        override fun canEnterRide(player: Player): Boolean =
            ride.canEnter(player).isAllowed() || player.isCrew()

        override fun getLastDispatch(): Long? = station.getLastDispatchTime()

        @JsonClass(generateAdapter = true)
        class Json(
            val sceneId: String
        ) : TracklesssRideBoardingDelegate.Json() {
            override fun create(tracklessRide: TracklessRide) =
                StationSceneBoardingDelegate(tracklessRide.scenes[sceneId] as TracklessStationScene)
        }
    }
}