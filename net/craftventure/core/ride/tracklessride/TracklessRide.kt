package net.craftventure.core.ride.tracklessride

import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor.serverError
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.logging.LogPriority
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.Logger.severe
import net.craftventure.core.ktx.util.Permissions
import net.craftventure.core.manager.Allow
import net.craftventure.core.manager.Deny
import net.craftventure.core.manager.GrantResult
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.metadata.GenericPlayerMeta
import net.craftventure.core.metadata.setLeaveLocation
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorAreaTracker
import net.craftventure.core.ride.operator.controls.ControlColors.NEUTRAL
import net.craftventure.core.ride.operator.controls.ControlColors.NEUTRAL_DARK
import net.craftventure.core.ride.operator.controls.OperatorButton
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.operator.controls.OperatorControl.ControlListener
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.trackedride.TrackedRideHelper.getAllowedToManuallyJoinRide
import net.craftventure.core.ride.trackedride.TracklessRideManager
import net.craftventure.core.ride.tracklessride.config.TracklessRideConfig
import net.craftventure.core.ride.tracklessride.navigation.GraphRouter
import net.craftventure.core.ride.tracklessride.navigation.NavigationGraph
import net.craftventure.core.ride.tracklessride.scene.TracklessStationScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.CarFactory
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import net.craftventure.database.generated.cvdata.tables.pojos.RideLog
import net.craftventure.database.repository.BaseIdRepository
import net.craftventure.database.type.RideLogState
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.Inventory
import java.time.LocalDateTime
import java.util.*

class TracklessRide(
    override val id: String,
    val area: Area,
    private val exitLocationProvider: (car: TracklessRideCar) -> Location,
    val graph: NavigationGraph,
    val router: GraphRouter,
    private val operatorSlots: Int = 1,
    private val operatorArea: Area = area,
    private val config: TracklessRideConfig,
    val controller: TracklessRideController = TracklessRideController(),
    val shooterRideContext: ShooterRideContext?,
    private val carFactoryProvider: (group: Int, car: Int) -> CarFactory
) : OperableRide, ControlListener {
    private var dbRide: Ride? = MainRepositoryProvider.rideRepository.getByName(id)
    var isFromConfig = false
    val world get() = area.world
    private var debugInventory: Inventory? = null

    private val destroyListeners = hashSetOf<DestroyListener>()
    val ejectLocationProvider = config.ejectLocations?.let { LocationListLocationProvider(world, it) }

    override val ride get() = dbRide

    private val rideQueues = hashSetOf<RideQueue>()

    init {
        area.loadChunks(true)
        controller.ride = this

        if (dbRide == null) {
            severe("Ride for " + id + " not found")
        }
        MainRepositoryProvider.rideRepository.addListener(object : BaseIdRepository.UpdateListener<Ride>() {
            override fun onUpdated(item: Ride) {
                if (item.name == id) {
                    dbRide = item
                }
            }
        })
    }

    fun addDestroyListener(destroyListener: DestroyListener) {
        destroyListeners += destroyListener
    }

    fun removeDestroyListener(destroyListener: DestroyListener) {
        destroyListeners -= destroyListener
    }

    fun addQueue(queue: RideQueue) {
        rideQueues.add(queue)
    }

    fun removeQueue(queue: RideQueue) {
        rideQueues.remove(queue)
    }

    override fun getQueues(): Set<RideQueue> = rideQueues

    val scenes = config.scenes.entries.associate { (key, data) ->
        key to data.toScene(this, key)
    }
    private var groups: Set<TracklessRideCarGroup> = setOf()

    init {
        graph.requirePrepared()
        val groupSize = config.settings.groupSize
        val groups = config.groups.map { (groupId, groupConfig) ->
            val scene = scenes.getValue(groupConfig.scene)
            require(groupConfig.cars.size == groupSize)
            val cars = groupConfig.cars.map { (carId, carConfig) ->
                val node = try {
                    graph.findNode(carConfig.startNode)!!
                } catch (e: Exception) {
                    throw IllegalStateException("Node $id/${carConfig.startNode} not found", e)
                }
                val pathPosition = try {
                    graph.toFirstPathPosition(node)!!
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "Node $id/${carConfig.startNode} has no proper links, you fucked up mate",
                        e
                    )
                }
                val generator = carFactoryProvider(groupId, carId)
                generator.produce(pathPosition, groupId, groupConfig, carId, carConfig, this)
            }
            TracklessRideCarGroup(
                groupId = groupId,
                cars = cars.toTypedArray(),
                currentScene = scene
            ).also {
                require(scene.enter(it))
            }
        }

        groups.forEach { group ->
            group.cars.forEach { car ->
                config.startActionsForAllCars.forEach { it.toAction().execute(this, group, car) }
                config.groups[group.groupId]!!.cars[car.idInGroup]!!.startActions.forEach { action ->
                    action.toAction().execute(this, group, car)
                }
            }
        }

        this.groups = groups.toSet()
    }

    private var emergencyStopActive = false
    private val operators = arrayOfNulls<Player>(operatorSlots)
    private val broadcastButton =
        OperatorButton("broadcast", OperatorButton.Type.DEFAULT, NEUTRAL, NEUTRAL_DARK, NEUTRAL).apply {
            setFlashing(true)
                .setName(CVTextColor.serverNoticeAccent + "Broadcast operated")
                .setDescription(CVTextColor.subtle + "Can be used every few minutes to let the server know you're operating this ride")
                .setSort(1)
                .setGroup(null)
                .setControlListener(this@TracklessRide)
            owner = this@TracklessRide
        }
    private val emergencyButton =
        OperatorButton("emergency", OperatorButton.Type.E_STOP).apply {
            setName(CVTextColor.serverNoticeAccent + "E-stop (Crew)")
                .setPermission(Permissions.CREW)
                .setDescription(CVTextColor.subtle + "Used by crewmembers to enable/disable an emergency stop")
                .setSort(2)
                .setGroup(null)
                .setControlListener(this@TracklessRide)
            owner = this@TracklessRide
        }
    override val operatorAreaTracker = OperatorAreaTracker(this, area)

    fun activeGroups(): Set<TracklessRideCarGroup> = groups

    init {
        if (dbRide == null) {
            severe("Ride for $id not found")
        }
        MainRepositoryProvider.rideRepository.addListener(object : BaseIdRepository.UpdateListener<Ride>() {
            override fun onUpdated(item: Ride) {
                if (id == item.name) {
                    dbRide = item
                }
            }
        })
    }


    fun openDebugMenu(player: Player) {
        if (debugInventory == null) {
            debugInventory = Bukkit.createInventory(null, 9 * 5, Component.text("Debug", CVTextColor.INVENTORY_TITLE))
//            for (i in rideTrains.indices) {
//                val rideTrain: RideTrain = rideTrains.get(i)
//                val trainStack = ItemStack(Material.GRAY_TERRACOTTA)
//                ItemStackUtils2.setDisplayName(trainStack, "§6" + rideTrain.javaClass.simpleName)
//                rideTrainsItems.add(trainStack)
//            }
        }
        player.openInventory(debugInventory!!)
    }

    fun hasPassengersInNonEnterableTrains(): Boolean {
        groups.forEach { group ->
            group.cars.forEach { car ->
                if (!car.canEnter && car.hasPlayers()) {
                    return true
                }
            }
        }
        return false
    }

    fun onPlayerEntered(player: Player, car: TracklessRideCar) {
        Logger.debug("${player.name} entered $id")
        scenes.forEach {
            it.value.onNotifyEnteredRide(player, car)
        }
        player.gameState()?.ride = this
    }

    fun onPlayerExited(player: Player, car: TracklessRideCar) {
        car.team?.remove(player)

        val finished = car.isEjecting
        if (finished) {
            executeAsync {
                MainRepositoryProvider.rideLogRepository.insertSilent(
                    RideLog(
                        UUID.randomUUID(), player.uniqueId, id, RideLogState.COMPLETED, LocalDateTime.now()
                    )
                )

                MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, config.finishAchievement)
                MainRepositoryProvider.rideCounterRepository.increaseCounter(player.uniqueId, id)
            }
        } else {
            executeAsync {
                MainRepositoryProvider.rideLogRepository.insertSilent(
                    RideLog(
                        UUID.randomUUID(),
                        player.uniqueId,
                        id,
                        RideLogState.LEFT,
                        LocalDateTime.now()
                    )
                )

                MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "early_leaver")
            }
        }
        Logger.debug("${player.name} exited $id finished=${car.isEjecting}")
        scenes.forEach {
            it.value.onNotifyExitedRide(player, car)
        }
        val exitProvider = car.group.currentScene.provideEjectOverride() ?: ejectLocationProvider
        if (exitProvider != null) {
            val location = exitProvider.provideEjectLocation(car, player)
            if (location != null) {
                player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
                player.setLeaveLocation(location)
            }
        }
        player.gameState()?.ride = null
    }

    fun handleInteract(player: Player, car: TracklessRideCar, seatEntityId: Int): Boolean {
        val rideQueue = car.group.currentScene.let { it as? TracklessStationScene }?.guestQueue
        if (rideQueue != null && rideQueue.isActive) {
            player.sendMessage(serverError + "At the moment you can only enter this ride by using the queue")
            return false
        }

        val enterState = canEnter(player, false)
        if (enterState is Deny) {
            player.sendMessage(Component.text("Can't enter: ${enterState.reason}", CVTextColor.serverError))
            return false
        }
        Logger.debug("Handle interact")
        if (!car.canEnter) {
//            if (player.isCrew())
//                player.sendMessage(serverError + "This car is currently restrained from entering, but as crew you can enter")
//            else
            return false
        }
        Logger.debug("Can enter, allowing interact")
        return car.tryToEnter(player, seatEntityId)
    }

    fun canEnter(player: Player, ignoreVehicle: Boolean = false): GrantResult {
        if (CraftventureCore.getOperatorManager().isOperatingSomewhere(player))
            return Deny("You're currently operating a ride")
        val joinRide = getAllowedToManuallyJoinRide(player)
        if (joinRide.isNotAllowed()) {
            return joinRide
        }
//        if (player.isInsideVehicle && !ignoreVehicle) {
//            return Deny("Must leave current vehicle first")
//        }
        if (CraftventureCore.getInstance().isShuttingDown) {
            return Deny("The server is shutting down")
        }
        if (dbRide == null) {
            return Deny("This ride is not properly setup")
        }
        if (!(dbRide!!.state!!.permission == null || player.hasPermission(dbRide!!.state!!.permission!!))) {
            return Deny("You don't meet the required permissions to enter this ride")
        }
        if (!dbRide!!.state!!.isOpen && !player.isCrew()) {
            return Deny("This ride isn't opened")
        }
        val cvMetadata = player.getMetadata<GenericPlayerMeta>()
        if (cvMetadata != null) {
            val lastExitLocation = cvMetadata.lastExitLocation
            if (lastExitLocation != null) {
                val playerLocation = player.location
                //                Logger.console("Checking last exit location %s vs %s and %s vs %s",
//                        lastExitLocation.getYaw(), playerLocation.getYaw(), lastExitLocation.getPitch(), playerLocation.getPitch());
                if (lastExitLocation.yaw == playerLocation.yaw || lastExitLocation.pitch == playerLocation.pitch) {
//                    Logger.console("Deny");
                    return Deny("This action is currently unavailable")
                }
            }
        }
        return Allow
    }

//    @Volatile
//    private var task: Int? = null

    fun start() {
        TracklessRideManager.add(this)
//        if (task != null) return
//        task = executeSync(1, 1) {
//            update()
//        }
    }

    fun stop() {
        TracklessRideManager.remove(this)
//        if (task == null) return
//        Bukkit.getScheduler().cancelTask(task!!)
//        task = null
    }

    fun destroy() {
        destroyListeners.forEach { it.onDestroy(this) }
        groups.forEach { group ->
            group.destroy()
        }
        scenes.forEach { (id, scene) ->
            scene.destroy()
        }
        shooterRideContext?.destroy()
    }

    fun updateAsync() {
        controller.updateAsync()
        scenes.forEach { (t, u) -> u.updateAsync() }
    }

    private var lastUpdate = System.currentTimeMillis()
    fun update() {
//        logcat { "Tick at ${Bukkit.getServer().currentTick}" }
        var updateCount = 0
        val tickTime = CraftventureCore.getSettings().coasterTickTime
//        int tickTimeMargin = CraftventureCore.getSettings().getCoasterTickTimeMargin();
        //        int tickTimeMargin = CraftventureCore.getSettings().getCoasterTickTimeMargin();
        while (updateCount == 0 || lastUpdate + tickTime < System.currentTimeMillis()) {
//            lastUpdate = System.currentTimeMillis();
            lastUpdate += tickTime.toLong()
            updateCount++

            controller.update()
            scenes.forEach { (t, u) -> u.update() }
            shooterRideContext?.update()

            if (updateCount > 200) {
                logcat(LogPriority.WARN) { "Skipping TracklessRide frames for $id because 200 updates were triggered this round" }
                break
            }
        }


        val debugInventory = debugInventory
        if (debugInventory != null && debugInventory.viewers.size > 0) {
            var index = 0
            scenes.forEach {
                val item = debugInventory.getItem(index)
                val newItem = it.value.createOrUpdateDebugItem(item)
                debugInventory.setItem(index, newItem)
                index++
            }
        }
    }

    override val isBeingOperated: Boolean
        get() = operators.any { it != null }

    override fun getOperatorForSlot(slot: Int): Player? = operators.getOrNull(slot)
    override fun getOperatorSlot(player: Player): Int = operators.indexOf(player)

    override fun setOperator(player: Player, slot: Int): Boolean {
        if (slot >= operators.size) return false
        if (operators[slot] == null) {
            operators[slot] = player
            player.sendMessage(CVTextColor.serverNotice + "You are now operating ${dbRide?.displayName}")
            return true
        }
        return false
    }

    override val totalOperatorSpots: Int
        get() = operatorSlots

    override fun cancelOperating(slot: Int) {
        if (slot >= operators.size) return
        operators[slot]?.sendMessage(CVTextColor.serverNotice + "You are no longer operatign ${dbRide?.displayName}")
        operators[slot] = null
    }

    override fun isInOperateableArea(location: Location): Boolean = location in operatorArea

    override fun onClick(
        operableRide: OperableRide,
        player: Player?,
        operatorControl: OperatorControl,
        operatorSlot: Int?
    ) {
        if (!operatorControl.isEnabled) {
            return
        }
        if (operatorControl === emergencyButton) {
            if (PermissionChecker.isCrew(player!!)) {
                activateEmergencyStop(!isEmergencyStopActive())
            } else {
                player.sendMessage(CVTextColor.serverError + "E-Stop usage is only accessible to crew")
            }
        } else if (operatorControl === broadcastButton) {
            CraftventureCore.getOperatorManager().broadcastRideOperated(this)
        }
    }

    open fun isEmergencyStopActive(): Boolean {
        return emergencyStopActive
    }

    open fun activateEmergencyStop(emergencyStopActive: Boolean) {
        if (this.emergencyStopActive != emergencyStopActive) {
            this.emergencyStopActive = emergencyStopActive
            onEmergencyStopChanged(emergencyStopActive)
        }
    }

    protected open fun onEmergencyStopChanged(emergencyStopActive: Boolean) {}

    override fun provideControls(): List<OperatorControl> {
        val controls: MutableList<OperatorControl> = ArrayList()
        controls.add(broadcastButton)
        controls.add(emergencyButton)
        scenes.forEach { (t, scene) ->
            controls.addAll(scene.provideControls())
        }
        return controls
    }

    override fun updateWhileOperated() {
    }

    fun provideEjectLocationProvider(car: TracklessRideCar): EjectLocationProvider? {
        return car.group.currentScene.provideEjectOverride() ?: ejectLocationProvider
    }

    interface EjectLocationProvider {
        fun provideEjectLocation(car: TracklessRideCar, who: Player): Location?

        companion object {
            val empty = object : EjectLocationProvider {
                override fun provideEjectLocation(car: TracklessRideCar, who: Player): Location? = null
            }
        }
    }

    class LocationListLocationProvider(val world: World, val locations: Map<String, List<Location>>) :
        EjectLocationProvider {
        override fun provideEjectLocation(car: TracklessRideCar, who: Player): Location? {
            val list = locations[car.idInGroup.toString()] ?: locations["*"]
            if (list == null || list.isEmpty()) return null
            return list.random()
        }
    }

    fun interface DestroyListener {
        fun onDestroy(ride: TracklessRide)
    }
}