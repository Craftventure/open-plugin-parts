package net.craftventure.core.ride.flatride

import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.extension.powerAsLever
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.animation.keyframed.DoubleValueKeyFrame
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.isOwnedByRide
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorAreaTracker
import net.craftventure.core.ride.operator.controls.*
import net.craftventure.core.script.ScriptManager
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.SimpleInterpolator
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import penner.easing.Cubic
import penner.easing.Linear
import penner.easing.Sine


class GonBaoCastle private constructor() : Flatride<GonBaoCastle.GonBaoBench>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    "gonbao",
    "ride_gonbao"
), OperableRide, OperatorControl.ControlListener {
    private val heightFrames = ArrayList<DoubleValueKeyFrame>()
    private val heightFramesInterpolations = ArrayList<SimpleInterpolator>()
    private var heightFrameIndex = 0
    private var sceneIndex = 0
    private val managedScenes = ArrayList<ManagedScene>()
    private var interpolatedHeight = 0.0
    private var lastProgramTime: Long = 0
    private val showController: ShowController

    private val preshowGates = arrayOf(
    )

    private val rideGates = arrayOf(
    )

    private val exitGates = arrayOf(
    )

    private val preshowExitLocation = Location(Bukkit.getWorld("world"),)

    //    private SimpleArea teleportArea = new SimpleArea("world", 341, 36, -616, 360, 47, -610);
    private val preshowArea = SimpleArea("world",)
    private val areaTracker = NpcAreaTracker(SimpleArea("world", ))
    private val benchFloors = ArrayList<BenchFloor>()

    private val rideArea1 = SimpleArea("world",)
    private val rideArea2 = SimpleArea("world",)
    private val rideArea3 = SimpleArea("world",)

    private val leverShow = Location(Bukkit.getWorld("world"), -).block
    private val operatorArea = CombinedArea(
//        if (CraftventureCore.isTestServer())
//            SimpleArea("world", -1000.0, 0.0, -1000.0, 1000.0, 300.0, 1000.0)
//        else
        SimpleArea("world", )
    )
    override val operatorAreaTracker: net.craftventure.bukkit.ktx.area.AreaTracker =
        OperatorAreaTracker(this, operatorArea)
    private val ledRunning = OperatorLed("running_indicator", ControlColors.NEUTRAL_DARK).apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Running"
        description = CVTextColor.MENU_DEFAULT_LORE + "Indicates wether the ride is running or not"
        setControlListener(this@GonBaoCastle)
    }

    private var preshowGatesOpen = false
    private val buttonPreshowGates = OperatorSwitch("preshowGates").apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Preshow entry gates"
        description = CVTextColor.MENU_DEFAULT_LORE + "Allow people to enter into the preshow area"
        setControlListener(this@GonBaoCastle)
        group = "preshow"
        sort = 1
    }

    private val buttonPreshowStart = OperatorButton("preshow", OperatorButton.Type.DEFAULT).apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Start preshow"
        isFlashing = true
        description =
            CVTextColor.MENU_DEFAULT_LORE + "Starts the preshow if it's not started yet. Requires gates to be closed"
        setControlListener(this@GonBaoCastle)
        group = "preshow"
        sort = 2
    }

    private val buttonDispatch = OperatorButton("dispatcher", OperatorButton.Type.DEFAULT).apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Start ride"
        isFlashing = true
        description =
            CVTextColor.MENU_DEFAULT_LORE + "Starts the ride if it's not started yet. Requires gates to be closed"
        setControlListener(this@GonBaoCastle)
        group = "ride"
        sort = 2
    }

    private var rideGatesOpen = false
    private val buttonRideGates = OperatorSwitch("rideGates").apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Gates to the ride"
        description = CVTextColor.MENU_DEFAULT_LORE + "Allow people to enter from the preshow to the ride"
        setControlListener(this@GonBaoCastle)
        group = "ride"
        sort = 1
    }

    private var exitGatesOpen = false
    private val buttonExitGates = OperatorSwitch("exitGates").apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Exit gates"
        description = CVTextColor.MENU_DEFAULT_LORE + "Allow people to exit the ride area"
        setControlListener(this@GonBaoCastle)
        group = "ride"
        sort = 3
    }

    private var operator: Player? = null
    protected var rideAngle: Double = 0.0
        private set

    init {
        for (entity in area.loc1.world!!.entities) {
            if (entity is ArmorStand && area.isInArea(entity.getLocation()) && entity.getCustomName() != null && (entity.getCustomName() == "gbc" || entity.getCustomName() == rideName)) {
                entity.remove()
            }
        }

        val seatSpacing = 1.0
        showController = ShowController()
        setupFrames()
        interpolatedHeight = heightFrames[0].value
        stopAllScenes()
        heightFrames.sortWith(Comparator { o1, o2 -> (o1.time - o2.time).toInt() })
        managedScenes.sortWith(Comparator { o1, o2 -> (o1.trigger - o2.trigger).toInt() })

        benchFloors.add(BenchFloor(-1.0, 0.0, SimpleArea("world",)))
        benchFloors.add(BenchFloor(0.0, 1.0, SimpleArea("world", )))
        benchFloors.add(BenchFloor(0.0, -1.0, SimpleArea("world", )))
        benchFloors.add(BenchFloor(1.0, 0.0, SimpleArea("world", )))

        for (benchFloor in benchFloors) {
            benchFloor.showBlocks(true)
        }

        cars.clear()
        cars.add(
            GonBaoBench(
                Location(area.loc1.world,),
                Vector(seatSpacing, 0.0, 0.0)
            )
        )
        cars.add(
            GonBaoBench(
                Location(area.loc1.world, ),
                Vector(seatSpacing, 0.0, 0.0)
            )
        )

        cars.add(
            GonBaoBench(
                Location(area.loc1.world, ),
                Vector(0.0, 0.0, -seatSpacing)
            )
        )

        cars.add(
            GonBaoBench(
                Location(area.loc1.world, ),
                Vector(-seatSpacing, 0.0, 0.0)
            )
        )
        cars.add(
            GonBaoBench(
                Location(area.loc1.world, ),
                Vector(-seatSpacing, 0.0, 0.0)
            )
        )

        cars.add(
            GonBaoBench(
                Location(area.loc1.world,),
                Vector(0.0, 0.0, seatSpacing)
            )
        )

        updateCarts(true)
        openPreshowGates(true)
        openRideGates(false)
        openExitGates(false)
        updatePreshowGatesButton()
        updateDispatchButton()
        updatePreshowStartButton()
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this::updatePreShow, 1, 1)
        //        start();
    }

    private fun updatePreShow() {
        showController.update()
    }

    private fun stopAllScenes() {
        for (managedScene in managedScenes) {
            managedScene.stop()
        }
    }

    private fun toMillis(seconds: Double): Double {
        return seconds * 1000
    }

    private fun setupFrames() {
        addFrame(0,0,Linear::easeInOut)
        addFrame(0,0, Linear::easeInOut)
        addFrame(0,0, Linear::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Sine::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)
        addFrame(0,0, Cubic::easeInOut)

        addScenePlay("gon", "scene1", 0.0)
        addSceneStop("gon", "scene1", 30.0)
        addScenePlay("gon", "scene2", 26.75)
        addSceneStop("gon", "scene2", 70.0)
    }

    private fun addScenePlay(groupId: String, name: String, time: Double) {
        managedScenes.add(ManagedScene(groupId, name, toMillis(time)))
    }

    private fun addSceneStop(groupId: String, name: String, time: Double) {
        managedScenes.add(ManagedScene(groupId, name, toMillis(time)).shouldStopOnExecute())
    }

    private fun addFrame(time: Double, height: Double, simpleInterpolator: SimpleInterpolator) {
        heightFrames.add(DoubleValueKeyFrame(toMillis(time), height))
        heightFramesInterpolations.add(simpleInterpolator)
    }

    private fun updateRunningIndicator() {
        ledRunning.color = if (isRunning()) ControlColors.NEUTRAL else ControlColors.NEUTRAL_DARK
        ledRunning.isFlashing = isRunning()
    }

    private fun updatePreshowGatesButton() {
        buttonPreshowGates.isEnabled = showController.state != ShowState.PLAYING
        buttonPreshowGates.isOn = preshowGatesOpen
    }

    private fun updatePreshowStartButton() {
        buttonPreshowStart.isEnabled = showController.state == ShowState.IDLE && !preshowGatesOpen && !rideGatesOpen
    }

    private fun updateRideGatesButton() {
        buttonRideGates.isEnabled = !isRunning() && showController.state != ShowState.PLAYING
        buttonRideGates.isOn = rideGatesOpen
    }

    private fun updateDispatchButton() {
        buttonDispatch.isEnabled = !isRunning() && !rideGatesOpen && !exitGatesOpen
    }

    private fun updateExitGatesButton() {
        buttonExitGates.isEnabled = !isRunning()
        buttonExitGates.isOn = exitGatesOpen
    }

    private fun openPreshowGates(open: Boolean) {
        this.preshowGatesOpen = open
        preshowGates.forEach { it.block.open(open) }
        updatePreshowGatesButton()
        updatePreshowStartButton()
    }

    private fun openRideGates(open: Boolean) {
        this.rideGatesOpen = open
        rideGates.forEach {
            it.block.open(open)
        }
        updateRideGatesButton()
        updateDispatchButton()
        updatePreshowStartButton()
    }

    private fun openExitGates(open: Boolean) {
        this.exitGatesOpen = open

        exitGates.forEach { block ->
            if (!open) {
                block.type = Material.SPRUCE_LOG
            } else {
                block.type = Material.AIR
            }
        }
        updateExitGatesButton()
        updateDispatchButton()
    }

    private fun tryOperatorStart() {
        if (!isRunning() && buttonDispatch.isEnabled) {
            start()
        }
    }

    private fun tryOpenPreshowGatesIfPossible(open: Boolean) {
        if (buttonPreshowGates.isEnabled) {
            openPreshowGates(open)
        }
    }

    private fun tryOpenRideGatesIfPossible(open: Boolean) {
        if (buttonRideGates.isEnabled) {
            openRideGates(open)
        }
    }

    private fun tryOpenExitGatesIfPossible(open: Boolean) {
        if (buttonExitGates.isEnabled) {
            openExitGates(open)
        }
    }

    override fun start() {
        super.start()
        updateRunningIndicator()
        updateDispatchButton()
        updatePreshowGatesButton()
        updateRideGatesButton()
        updateExitGatesButton()
    }

    override fun stop() {
        super.stop()
        heightFrameIndex = 0
        AudioServerApi.disable("gonbao_onride")

        stopAllScenes()
        areaTracker.stopTracking()
        if (!isBeingOperated) {
            openExitGates(true)
            if (showController.state == ShowState.WAITING_FOR_DOORS)
                openRideGates(true)
        }

        updateRunningIndicator()
        updateDispatchButton()
        updatePreshowGatesButton()
        updateRideGatesButton()
        updateExitGatesButton()
    }

    override val isBeingOperated: Boolean
        get() = operator != null

    override fun getOperatorForSlot(slot: Int): Player? = when (slot) {
        0 -> operator
        else -> null
    }

    override fun getOperatorSlot(player: Player): Int = when {
        player === operator -> 0
        else -> -1
    }

    override fun setOperator(player: Player, slot: Int): Boolean {
        if (slot == 0 && operator === null) {
            operator = player
            operator?.sendMessage(CVTextColor.serverNotice + "You are now operating " + ride?.displayName)
            cancelAutoStart()
            return true
        }
        return false
    }

    override val totalOperatorSpots: Int
        get() = 1

    override fun cancelOperating(slot: Int) {
        if (slot == 0) {
            operator?.let {
                it.sendMessage(CVTextColor.serverNotice + "You are no longer operating " + ride?.displayName)
                operator = null
                scheduleAutoStart()

                tryOpenPreshowGatesIfPossible(showController.state == ShowState.IDLE)
                tryOpenRideGatesIfPossible(showController.state == ShowState.WAITING_FOR_RIDE_START)
//                tryOpenRideGatesIfPossible(showController.state == ShowState.WAITING_FOR_DOORS || showController.state == ShowState.WAITING_FOR_RIDE_START)
//                tryOpenPreshowGatesIfPossible(true)
            }
        }
    }

    override fun provideControls(): MutableList<OperatorControl> =
        mutableListOf(
            ledRunning,
            buttonPreshowGates,
            buttonPreshowStart,
            buttonRideGates,
            buttonExitGates,
            buttonDispatch
        )

    override fun isInOperateableArea(location: Location): Boolean = operatorArea.isInArea(location)

    override fun updateWhileOperated() {}

    override val id: String
        get() = rideName

    override fun onClick(
        operableRide: OperableRide,
        player: Player?,
        operatorControl: OperatorControl,
        operatorSlot: Int?
    ) {
        if (operatorControl.isEnabled) {
            if (operatorControl === buttonDispatch) {
                tryOperatorStart()
            } else if (operatorControl == buttonPreshowGates) {
                tryOpenPreshowGatesIfPossible(!buttonPreshowGates.isOn)
            } else if (operatorControl == buttonRideGates) {
                tryOpenRideGatesIfPossible(!buttonRideGates.isOn)
            } else if (operatorControl == buttonExitGates) {
                tryOpenExitGatesIfPossible(!buttonExitGates.isOn)
            } else if (operatorControl == buttonPreshowStart) {
                if (buttonPreshowStart.isEnabled)
                    showController.requestStart()
            }
        }
    }

    override fun prepareStart() {
        super.prepareStart()
        lastProgramTime = 0
        sceneIndex = 0
        heightFrameIndex = 0

        AudioServerApi.enable("gonbao_onride")
        AudioServerApi.sync("gonbao_onride", System.currentTimeMillis())
        areaTracker.startTracking()

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.vehicle?.isOwnedByRide(this) != true) {
                if (rideArea1.isInArea(player) ||
                    rideArea2.isInArea(player) ||
                    rideArea3.isInArea(player)
                ) {
                    player.teleport(exitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    sendMoveAwayMessage(player)
                }
            }
        }
        openExitGates(false)

        //        for (Player player : Bukkit.getOnlinePlayers()) {
        //            if (teleportArea.isInArea(player)) {
        //                if (!player.isInsideVehicle()) {
        //                    player.teleport(exitLocation);
        //                    sendMoveAwayMessage(player);
        //                }
        //            }
        //        }
        //        ScriptManager.start("gon", "scene1");
    }

    override fun provideRunnable(): FlatrideRunnable = object : FlatrideRunnable() {
        var frame = 0
        var hasAnimatedOutwards = false
        var hasAnimatedInwards = false

        override fun updateTick() {
            frame++
            val programTime = System.currentTimeMillis() - startTime
            while (sceneIndex < managedScenes.size && managedScenes[sceneIndex].check(
                    lastProgramTime,
                    programTime
                )
            ) {
                sceneIndex++
            }

            if (!hasAnimatedOutwards && programTime > 20000) {
                hasAnimatedOutwards = true
                for (benchFloor in benchFloors) {
                    benchFloor.animateAway()
                }
            }

            if (!hasAnimatedInwards && programTime > heightFrames[heightFrames.size - 1].time - 7000) {
                hasAnimatedInwards = true
                for (benchFloor in benchFloors) {
                    benchFloor.animateIn()
                }
            }

            if (update(heightFrames, heightFrameIndex, programTime))
                heightFrameIndex++

            interpolatedHeight =
                interpolate(heightFrames, heightFramesInterpolations, heightFrameIndex, programTime)

            if (frame > 2) {
                frame = 0
                if (programTime > heightFrames[heightFrames.size - 1].time) {
                    updateCarts(true)
                    stop()
                    return
                }

                updateCarts(false)
            }
            lastProgramTime = programTime
        }
    }

    override fun updateCarts(forceTeleport: Boolean) {
        for (seat in cars) {
            seat.teleport(
                0.0,
                interpolatedHeight,
                0.0,
                0f,
                0f,
                forceTeleport
            )
        }
    }

    private fun interpolate(
        frames: List<DoubleValueKeyFrame>,
        interpolators: List<SimpleInterpolator>,
        index: Int,
        time: Long
    ): Double {
        if (frames.size > index + 1) {
            val current = frames[index]
            val next = frames[index + 1]

            val interpolator = interpolators[index]
            //            DecimalFormat decimalFormat = new DecimalFormat("####,##");

            val t = interpolator.interpolate(
                (time - current.time).toFloat().toDouble(),
                0.0, 1.0, (next.time - current.time).toFloat().toDouble()
            )

            //            Logger.console(String.format("Interpolate %s, %s, %s, %s = %s",
            //                    decimalFormat.format(time - current.getTime()),
            //                    decimalFormat.format(current.getValue()),
            //                    decimalFormat.format(next.getValue()),
            //                    decimalFormat.format((next.getTime() - current.getTime())),
            //                    decimalFormat.format(t)));
            //            Logger.console(index + "Height > " + height + " for " + current.getTime() + ", " + current.getValue() + " >> " + next.getTime() + ", " + next.getValue());
            return InterpolationUtils.linearInterpolate(current.value, next.value, t)
        } else {
            return frames[index].value
        }
    }

    private fun update(frames: List<DoubleValueKeyFrame>, index: Int, time: Long): Boolean {
        return frames.size > index + 1 && frames[index + 1].time <= time
    }

    protected inner class ShowController {
        var state = ShowState.IDLE
            private set(newState) {
                if (this.state != newState) {
//                    Logger.info("GonBao preshow state ${newState.name}")
                    field = newState
                    if (this.state == ShowState.IDLE) {
                        stop()
                    }
                    inStateSince = System.currentTimeMillis()

                    if (!isBeingOperated) {
                        openPreshowGates(state == ShowState.IDLE)
                        openRideGates(state == ShowState.WAITING_FOR_RIDE_START)
                    }

//                    preshowEntranceDoorA.block.open(state == ShowState.IDLE)
//                    preshowEntranceDoorB.block.open(state == ShowState.IDLE)
//
//                    preshowExitDoorA.block.open(state == ShowState.WAITING_FOR_RIDE_START)
//                    preshowExitDoorB.block.open(state == ShowState.WAITING_FOR_RIDE_START)
//                    preshowExitDoorC.block.open(state == ShowState.WAITING_FOR_RIDE_START)
//                    preshowExitDoorD.block.open(state == ShowState.WAITING_FOR_RIDE_START)

                    if (state == ShowState.PLAYING) {
                        start()

                        leverShow.powerAsLever(false)
                    }
                    if (state == ShowState.WAITING_FOR_DOORS && this@GonBaoCastle.isRunning()) {
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (preshowArea.isInArea(player)) {
                                player.sendMessage(
                                    Translation.RIDE_GONBAO_PRESHOW_WAITING_FOR_RIDE.getTranslation(
                                        player
                                    )!!
                                )
                            }
                        }
                    }
                    updatePreshowStartButton()
                    updatePreshowGatesButton()
                    updateRideGatesButton()
                }
            }
        private var inStateSince = System.currentTimeMillis()
        private var gatheringPlayerStartingTime: Long = 0

        init {
//            preshowEntranceDoorA.block.open(true)
//            preshowEntranceDoorB.block.open(true)
        }

        fun requestStart(): Boolean {
            if (state == ShowState.IDLE) {
                state = ShowState.PLAYING
                return true
            }
            return false
        }

        fun update() {
            if (!isBeingOperated) {
                if (state == ShowState.PLAYING && rideGatesOpen) {
                    openRideGates(false)
                }
                if (state != ShowState.IDLE && preshowGatesOpen) {
                    openPreshowGates(false)
                }
            }
//            if (state != ShowState.IDLE && isBeingOperated) {
//                state = ShowState.IDLE
//                return
//            }
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
                                "Please wait a few seconds for Mr. Gon Bao to prepare his briefing...",
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
                if (!isBeingOperated) {
                    if (gatheringPlayerStartingTime > 0) {
                        if (gatheringPlayerStartingTime < System.currentTimeMillis() - 15000) {
                            state = ShowState.PLAYING
                        }
                    }
                }
            } else if (state == ShowState.PLAYING) {
                if (inStateSince < System.currentTimeMillis() - 80000) {
                    state = ShowState.WAITING_FOR_DOORS
                }
            } else if (state == ShowState.WAITING_FOR_DOORS) {
                if (!this@GonBaoCastle.isRunning() && (!isBeingOperated || rideGatesOpen)) {
                    state = ShowState.WAITING_FOR_RIDE_START
                } else {
                    for (player in Bukkit.getOnlinePlayers()) {
                        val inArea = preshowArea.isInArea(player)
                        if (inArea) {
                            display(
        player,
        Message(
            id = ChatUtils.ID_RIDE,
            text = Component.text(
                                    "Mr. Gon Bao is preparing the machine, please wait a moment...",
                                    CVTextColor.serverNotice
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
                if (this@GonBaoCastle.isRunning() || inStateSince < System.currentTimeMillis() - 5000) {
                    //                    Logger.console("GonBao preshow resetting, has ride started? " + GonBaoCastle.this.isRunning());
                    state = ShowState.IDLE
                }
            }
        }

        private fun start() {
            ScriptManager.start("gon", "preshow")
            AudioServerApi.enable("gonbao_preshow")
            AudioServerApi.sync("gonbao_preshow", System.currentTimeMillis())
        }

        private fun stop() {
            if (!isBeingOperated) {
                for (player in Bukkit.getOnlinePlayers()) {
                    if (preshowArea.isInArea(player)) {
                        player.teleport(preshowExitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    }
                }
            }

            ScriptManager.stop("gon", "preshow")
            AudioServerApi.disable("gonbao_preshow")
        }
    }

    protected enum class ShowState {
        IDLE, PLAYING, WAITING_FOR_DOORS, WAITING_FOR_RIDE_START
    }

    protected class ManagedScene(val groupId: String, val name: String, val trigger: Double) {
        private var shouldStop: Boolean = false

        fun shouldStopOnExecute(): ManagedScene {
            this.shouldStop = true
            return this
        }

        fun check(previousTime: Long, newTime: Long): Boolean {
            if (previousTime <= trigger && newTime > trigger) {
                if (shouldStop)
                    ScriptManager.stop(groupId, name)
                else
                    ScriptManager.start(groupId, name)
                return true
            }
            return false
        }

        fun stop() {
            ScriptManager.stop(groupId, name)
        }
    }

    protected inner class BenchFloor(val xDirection: Double, val zDirection: Double, area: SimpleArea) {
        private var bukkitTaskId = -1
        private val locations = ArrayList<Location>()
        private val npcEntities = ArrayList<NpcEntity>()

        init {
            var x = area.loc1.x.toInt()
            while (x <= area.loc2.x) {
                var z = area.loc1.z.toInt()
                while (z <= area.loc2.z) {
                    val block = Location(area.loc1.world, x.toDouble(), 42.0, z.toDouble()).block
                    //                    Logger.console(block.getType().name() + " > " + block.getData());
                    if (block.type == Material.DARK_OAK_SLAB || block.type == Material.AIR) {
                        locations.add(block.location.add(0.5, 0.0, 0.5))
                    }
                    z++
                }
                x++
            }
        }

        private fun clearEntities() {
            for (npcEntity in npcEntities) {
                areaTracker.removeEntity(npcEntity)
            }
            npcEntities.clear()
        }

        fun showBlocks(show: Boolean) {
            for (location in locations) {
                if (!show) {
                    location.block.type = Material.AIR
                } else {
                    location.block.type = Material.DARK_OAK_SLAB
                }
            }
        }

        private fun cancel() {
            Bukkit.getScheduler().cancelTask(bukkitTaskId)
            clearEntities()
        }

        private fun getXOffset(t: Double): Double {
            return 3.0 * xDirection * t
        }

        private fun getYOffset(t: Double): Double {
            return t * -0.52
        }

        private fun getZOffset(t: Double): Double {
            return 3.0 * zDirection * t
        }

        private fun moveBlocks(t: Double) {
            //            Logger.console("T " + t + " > " + npcEntities.size());
            for (npcEntity in npcEntities) {
                val location = npcEntity.tag as Location
                npcEntity.move(
                    location.x + getXOffset(t),
                    location.y + getYOffset(t),
                    location.z + getZOffset(t)
                )
            }
        }

        fun animateAway() {
            cancel()
            showBlocks(false)
            val startTime = System.currentTimeMillis()
            val endTime = System.currentTimeMillis() + 5000
            bukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                if (npcEntities.size == 0) {
                    for (location in locations) {
                        val npcEntity = NpcEntity(
                            "gonBaoDoor",
                            EntityType.FALLING_BLOCK, location.clone().add(
                                getXOffset(0.0),
                                getYOffset(0.0),
                                getZOffset(0.0)
                            )
                        )
                        npcEntity.noGravity(true)
                        npcEntity.setBlockData(Material.DARK_OAK_SLAB.createBlockData())
                        npcEntities.add(npcEntity)
                        areaTracker.addEntity(npcEntity)
                        npcEntity.tag = location.clone()
                    }
                }
                if (endTime < System.currentTimeMillis()) {
                    cancel()
                    return@scheduleSyncRepeatingTask
                }
                val t = Sine.easeIn(
                    (System.currentTimeMillis() - startTime).toDouble(),
                    0.0,
                    1.0,
                    (endTime - startTime).toDouble()
                )
                moveBlocks(t)
            }, 1, 1)
        }

        fun animateIn() {
            cancel()
            val startTime = System.currentTimeMillis()
            val endTime = System.currentTimeMillis() + 5000
            bukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                if (npcEntities.size == 0) {
                    for (location in locations) {
                        val npcEntity = NpcEntity(
                            "gonBaoDoor",
                            EntityType.FALLING_BLOCK, location.clone().add(
                                getXOffset(1.0),
                                getYOffset(1.0),
                                getZOffset(1.0)
                            )
                        )
                        npcEntity.setBlockData(Material.DARK_OAK_SLAB.createBlockData())
                        npcEntities.add(npcEntity)
                        areaTracker.addEntity(npcEntity)
                        npcEntity.tag = location.clone()
                    }
                }
                if (endTime < System.currentTimeMillis()) {
                    cancel()
                    showBlocks(true)
                    return@scheduleSyncRepeatingTask
                }
                val t = 1 - Sine.easeIn(
                    (System.currentTimeMillis() - startTime).toDouble(),
                    0.0,
                    1.0,
                    (endTime - startTime).toDouble()
                )
                moveBlocks(t)
            }, 1, 1)
        }
    }

    inner class GonBaoBench constructor(private val loc: Location, private val rightVector: Vector) :
        FlatrideCar<ArmorStand>(arrayOfNulls(5)) {

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            var forceTeleport = forceTeleport
            forceTeleport = true// TODO: Remove debug
            for (i in entities.indices) {
                if (entities[i] == null || !entities[i].isValid) {
                    entities[i] = if (i == 0)
                        loc.world!!.spawn(loc.clone().add(rightVector.clone().multiply(-1.6)), ArmorStand::class.java)
                    else if (i == 1)
                        loc.world!!.spawn(loc.clone().add(rightVector.clone().multiply(-0.8)), ArmorStand::class.java)
                    else if (i == 2)
                        loc.world!!.spawn(loc.clone().add(rightVector.clone().multiply(0)), ArmorStand::class.java)
                    else if (i == 3)
                        loc.world!!.spawn(loc.clone().add(rightVector.clone().multiply(0.8)), ArmorStand::class.java)
                    else
                        loc.world!!.spawn(loc.clone().add(rightVector.clone().multiply(1.6)), ArmorStand::class.java)
                    entities[i].isPersistent = false
                    entities[i]?.getOrCreateMetadata { TypedInstanceOwnerMetadata(ride = this@GonBaoCastle) }
                    entities[i].setGravity(false)
                    entities[i].setBasePlate(false)
                    entities[i].isVisible = false
                    entities[i].customName = rideName
                    entities[i].addDisabledSlots(*EquipmentSlot.values())
                    if (i == 2)
                        entities[i].setHelmet(MaterialConfig.GON_BAO_BENCH)
                } else if (i == 2 || entities[i].passenger != null || forceTeleport) {
                    EntityUtils.teleport(
                        entities[i],
                        entities[i].location.x,
                        y,
                        entities[i].location.z
                    )
                } else if (entities[i].passenger == null && entities[i].location.y > 5) {
                    val location = entities[i].location
                    EntityUtils.teleport(entities[i], location.x, 5.0, location.z, location.yaw, location.pitch)
                }

                if (forceModelUpdate) {
                    if (i == 2)
                        entities[i].setHelmet(MaterialConfig.GON_BAO_BENCH)
                }
            }
        }

    }

    companion object {
        private var _instance: GonBaoCastle? = null

        fun getInstance(): GonBaoCastle {
            if (_instance == null) {
                _instance = GonBaoCastle()
            }
            return _instance!!
        }
    }
}
