package net.craftventure.core.ride.tracklessride.scene

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorManager
import net.craftventure.core.ride.operator.controls.OperatorButton
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.operator.controls.OperatorSwitch
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.StationHoldProgramPart
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.craftventure.core.utils.OperatorUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.time.Duration

class TracklessStationScene(
    id: String,
    tracklessRide: TracklessRide,
    private val data: Data,
) : TracklessRideScene(id, tracklessRide, data) {
    private val buttonDispatch = OperatorButton(id + "_dispatcher", OperatorButton.Type.DEFAULT).apply {
        owner = this@TracklessStationScene
        group = data.controlGroupId ?: id
        isFlashing = true
        sort = 1
        setType(OperatorButton.Type.DISPATCH)
        name = CVTextColor.MENU_DEFAULT_TITLE + "Dispatch ${data.displayName}"
        description = CVTextColor.MENU_DEFAULT_LORE + "Requires gates to be closed and/or harness to be closed"
        setControlListener(this@TracklessStationScene)
    }

    private var gatesOpen = false
    private var harnessOpen = false
    private val buttonGates: OperatorSwitch
    private val buttonHarness: OperatorSwitch

    private var lastDepartureTime: Long = 0
    private var dispatchInterval: Long = 0
    private var lastStatusMessageUpdateTime: Long = 0
    private var keepRollingTime: Long = 0
    private var minimumHoldTime: Long = 5000

    private var playersEnteredTime: Long = 0
    private var playerEnterAutoDispatchDelay: Long = 10000
    private val decimal2ZeroesFormatter = DecimalFormat("#")

    private var debugInstantStart = false
    private var lastHarnessClickTime: Long = 0
    private var lastGatesClickTime: Long = 0
    private var lastStateSwitch: Long = 0

    private val ejectLocationProvider =
        data.ejectLocations?.let { TracklessRide.LocationListLocationProvider(tracklessRide.world, it) }

    var isHarnessSounds = true
    var autoDispatchTime = 0L

    var guestQueue: RideQueue? = null

    val estimatedMillisecondsUntil: Long?
        get() {
            if (this.state != State.HOLDING)
                return null
            var estimate: Long = -1
            if (dispatchInterval > 0) {
                estimate = lastDepartureTime + dispatchInterval - System.currentTimeMillis()
            }

            if (playersEnteredTime > 0) {
                estimate =
                    Math.max(estimate, playersEnteredTime + playerEnterAutoDispatchDelay - System.currentTimeMillis())
            }

            return if (estimate < 0) -1 else estimate
        }

    fun getLastDispatchTime() = lastDepartureTime.takeIf { it != 0L }

    init {
        buttonGates = OperatorSwitch(id + "_gates")
        buttonGates.owner = this
        buttonGates
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Gates")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "Open/Close the gates when there is a train waiting for dispatch")
            .setSort(3)
            .setGroup(id)
            .setControlListener(this)

        buttonHarness = OperatorSwitch(id + "_harness")
        buttonHarness.owner = this
        buttonHarness
            .setType(OperatorSwitch.Type.LOCK_UNLOCK)
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Harness")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "(Un)Lock the harness when there is a train waiting for dispatch")
            .setSort(4)
            .setGroup(id)
            .setControlListener(this)
    }

    override fun createOrUpdateDebugItem(itemStack: ItemStack?): ItemStack {
        val item = super.createOrUpdateDebugItem(itemStack)
        item.loreWithBuilder {
            item.lore()?.let(::startWithLines)
            labeled("isMinimumHoldTimeSatisfied", isMinimumHoldTimeSatisfied(DispatchRequestType.AUTO).toString())
            labeled("isKeepRollingSatisfied", isKeepRollingSatisfied(DispatchRequestType.AUTO).toString())
            labeled("isDispatchIntervalSatisfied", isDispatchIntervalSatisfied(DispatchRequestType.AUTO).toString())
            labeled(
                "isPlayersEnteredDelaySatisfied",
                isPlayersEnteredDelaySatisfied(DispatchRequestType.AUTO).toString()
            )
            labeled("canProgressToNextScene", canProgressToNextScene().toString())
            labeled("playerEnterAutoDispatchDelay", playerEnterAutoDispatchDelay.toString())
            labeled(
                "lastStatusMessageUpdateTime",
                "$lastStatusMessageUpdateTime (Δ${System.currentTimeMillis() - lastStatusMessageUpdateTime})"
            )
            labeled("keepRollingTime", "$keepRollingTime")
            labeled("playersEnteredTime", "$playersEnteredTime (Δ${System.currentTimeMillis() - playersEnteredTime})")
            labeled("lastDepartureTime", "$lastDepartureTime (Δ${System.currentTimeMillis() - lastDepartureTime})")
            labeled("lastStateSwitch", "$lastStateSwitch (Δ${System.currentTimeMillis() - lastStateSwitch})")
            labeled("autoDispatchTime", "$autoDispatchTime (Δ${System.currentTimeMillis() - autoDispatchTime})")
        }
        return item
    }

    private val gateBlocks = data.gates.map { it.toLocation(tracklessRide.world).block }
    var state: State = State.IDLE
        private set(value) {
//            Logger.debug("Station state changing to $value")
            field = value
            currentGroup?.cars?.forEach { it.canEnter = value == State.HOLDING }

            if (state == State.HOLDING) {
                currentGroup?.cars?.forEach { car ->
                    car.eject()
                }
                if (!OperatorUtils.isBeingOperated(tracklessRide)) {
                    openGates(true)
                    openHarness(true)
                }
            } else {
                openGates(false)
                openHarness(false)

                currentGroup?.cars?.forEach { car ->
                    for (player in car.playerPassengers) {
                        MessageBarManager.remove(player, ChatUtils.ID_RIDE)
                    }
                }
            }

            if (state == State.DISPATCHING) {
                lastDepartureTime = System.currentTimeMillis()
            }
            playersEnteredTime = 0

            lastStateSwitch = System.currentTimeMillis()
            updateControls()
        }

    override fun provideControls(): List<OperatorControl> = listOf(
        buttonDispatch,
        buttonGates,
        buttonHarness,
    )

    override fun onClick(
        operableRide: OperableRide,
        player: Player?,
        operatorControl: OperatorControl,
        operatorSlot: Int?
    ) {
        super.onClick(operableRide, player, operatorControl, operatorSlot)

        Logger.debug("Click ${operatorControl.isEnabled} ${operatorControl.id}")

        if (operatorControl.isEnabled) {
            if (operatorControl === buttonDispatch) {
                tryOperatorStart()
            } else if (operatorControl === buttonGates) {
                if (lastGatesClickTime < System.currentTimeMillis() - OperatorManager.INTERACTION_TIMEOUT) {
                    lastGatesClickTime = System.currentTimeMillis()
                    tryOpenGatesIfPossible(!buttonGates.isOn)
                }
            } else if (operatorControl === buttonHarness) {
                if (lastHarnessClickTime < System.currentTimeMillis() - OperatorManager.INTERACTION_TIMEOUT) {
                    lastHarnessClickTime = System.currentTimeMillis()
                    tryOpenHarnessIfPossible(!buttonHarness.isOn)
                }
            }
        }
    }

    override fun provideEjectOverride(): TracklessRide.EjectLocationProvider? {
        if (state == State.HOLDING) {
            when (data.holdingEjectType) {
                EjectType.TO_SEAT -> return TracklessRide.EjectLocationProvider.empty
                else -> {}
            }
        }
        return ejectLocationProvider ?: super.provideEjectOverride()
    }

    override fun onCurrentGroupUpdated() {
        super.onCurrentGroupUpdated()
        if (currentGroup != null) {
            state = State.ENTERING
        } else {
            state = State.IDLE
        }
    }

    override fun update() {
        super.update()

        val currentGroup = currentGroup
//        stationButton.isFlashing = state == State.HOLDING
        if (currentGroup != null && state == State.ENTERING) {
            val allCarsHold =
                currentGroup.cars.all {
                    tracklessRide.controller.getProgramPartForCar(it)?.javaClass?.isAssignableFrom(
                        StationHoldProgramPart::class.java
                    ) == true
                }
            buttonDispatch.isEnabled = false
            if (allCarsHold) {
                state = State.HOLDING
            }
        } else if (currentGroup != null) {
            val allCarsHold =
                currentGroup.cars.all {
                    tracklessRide.controller.getProgramPartForCar(it)?.javaClass?.isAssignableFrom(
                        StationHoldProgramPart::class.java
                    ) == true
                }
            buttonDispatch.isEnabled = allCarsHold
        } else {
            buttonDispatch.isEnabled = false
        }

        if (lastStatusMessageUpdateTime < System.currentTimeMillis() - 500) {
            updateMessages()
        }

        if (this.state == State.HOLDING) {
//            Logger.debug("Updating hold ${playersEnteredTime>0} ${tracklessRide.hasPassengersInNonEnterableTrains()} ${debugInstantStart}")
            //            boolean isBeingOperated = OperatorUtils.isBeingOperated(getFlatride());
//            if (targetTrain != null && !targetTrain!!.canEnter()) {
//                targetTrain!!.setCanEnter(harnessOpen)
//            }

            if (playersEnteredTime > 0) {
                tryDispatchNow(DispatchRequestType.AUTO)
            } else if (tracklessRide.hasPassengersInNonEnterableTrains()) {
                tryDispatchNow(DispatchRequestType.AUTO_KEEP_ROLLING)
            } else if (debugInstantStart) {
                tryDispatchNow(DispatchRequestType.DEBUG_ROLLING_TEST)
            }
        }

        updateControls()

        if (currentGroup != null) {
            val passengerCount = currentGroup.playerPassengers.size
            if (passengerCount == 0 && playersEnteredTime != 0L) {
                playersEnteredTime = 0
            }
        }

        if (currentGroup != null)
            if (autoDispatchTime > 0 && autoDispatchTime < System.currentTimeMillis() - lastStateSwitch) {
                if (isDispatchIntervalSatisfied(DispatchRequestType.AUTO)) {
                    tryDispatchNow(DispatchRequestType.AUTO)
                }
            }

//        // DEBUG
//        if (state == State.HOLDING) {
//            state = State.DISPATCHING
//        }
    }

    fun setAutoDispatchTime(duration: Duration) {
        this.autoDispatchTime = duration.inWholeMilliseconds
    }

    fun setDebugInstantStart(debugInstantStart: Boolean) {
        this.debugInstantStart = debugInstantStart
    }

    fun setKeepRollingTime(duration: Duration) {
        this.keepRollingTime = duration.inWholeMilliseconds
    }

    fun setDispatchInterval(duration: Duration) {
        this.dispatchInterval = duration.inWholeMilliseconds
    }

    fun setAutoStartDelay(duration: Duration) {
        this.playerEnterAutoDispatchDelay = duration.inWholeMilliseconds
    }

    fun setMinimumHoldTime(duration: Duration) {
        this.minimumHoldTime = duration.inWholeMilliseconds
    }

    private fun updateMessage(player: Player) {
        if (this.state != State.HOLDING) {
            return
        }

        val rideName = if (tracklessRide.ride != null) tracklessRide.ride!!.displayName else tracklessRide.id
        if (OperatorUtils.isBeingOperated(tracklessRide)) {
            val operator = OperatorUtils.getOperatorForSlot(tracklessRide, 0)
            display(
                player,
                Message(
                    id = ChatUtils.ID_RIDE,
                    text = Translation.RIDE_WAITING_FOR_OPERATOR_DISPATCH.getTranslation(
                        player,
                        rideName,
                        operator?.name ?: "unknown"
                    )!!,
                    type = MessageBarManager.Type.RIDE,
                    untilMillis = TimeUtils.secondsFromNow(2.0),
                ),
                replace = true,
            )
        } else {
            val estimate = estimatedMillisecondsUntil
            if (estimate != null && estimate > 0) {
                display(
                    player,
                    Message(
                        id = ChatUtils.ID_RIDE,
                        text = Translation.RIDE_DISPATCH_IN_X_SECONDS.getTranslation(
                            player,
                            rideName,
                            decimal2ZeroesFormatter.format(ceil(estimate / 1000.0))
                        )!!,
                        type = MessageBarManager.Type.RIDE,
                        untilMillis = TimeUtils.secondsFromNow(2.0),
                    ),
                    replace = true,
                )
            } else {
                display(
                    player,
                    Message(
                        id = ChatUtils.ID_RIDE,
                        text = Translation.RIDE_WAITING_FOR_AUTO_DISPATCH.getTranslation(
                            player,
                            rideName
                        )!!,
                        type = MessageBarManager.Type.RIDE,
                        untilMillis = TimeUtils.secondsFromNow(1.0),
                    ),
                    replace = true,
                )
            }
        }
    }

    private fun isMinimumHoldTimeSatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return lastStateSwitch < System.currentTimeMillis() - minimumHoldTime || dispatchRequestType == DispatchRequestType.OPERATOR
    }

    private fun isDispatchIntervalSatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return dispatchInterval <= 0 || lastDepartureTime < System.currentTimeMillis() - dispatchInterval
    }

    private fun isPlayersEnteredDelaySatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return playersEnteredTime <= 0 || playersEnteredTime < System.currentTimeMillis() - playerEnterAutoDispatchDelay || dispatchRequestType == DispatchRequestType.OPERATOR
    }

    private fun isKeepRollingSatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return if (dispatchRequestType == DispatchRequestType.AUTO_KEEP_ROLLING) {
            keepRollingTime > 0 && lastDepartureTime < System.currentTimeMillis() - keepRollingTime
        } else true
    }

    override fun onNotifyEnteredRide(player: Player, car: TracklessRideCar) {
        super.onNotifyEnteredRide(player, car)
        if (car.group == currentGroup && playersEnteredTime == 0L) {
            playersEnteredTime = System.currentTimeMillis()
        }
    }

    override fun onNotifyExitedRide(player: Player, car: TracklessRideCar) {
        super.onNotifyExitedRide(player, car)
        MessageBarManager.remove(player, ChatUtils.ID_RIDE)
    }

    fun canDispatch(dispatchRequestType: DispatchRequestType): Boolean {
//        Logger.debug("canDispatch ${}")
        if (currentGroup == null) {
            return false
        }
        if (tracklessRide.isEmergencyStopActive()) return false
        val isBeingOperated = OperatorUtils.isBeingOperated(tracklessRide)
        if (dispatchRequestType != DispatchRequestType.OPERATOR && isBeingOperated) {
            return false
        } else if (dispatchRequestType == DispatchRequestType.OPERATOR && !isBeingOperated) {
            return false
        }
        return this.state == State.HOLDING &&
                isMinimumHoldTimeSatisfied(dispatchRequestType) &&
                isKeepRollingSatisfied(dispatchRequestType) &&
                isDispatchIntervalSatisfied(dispatchRequestType) &&
                isPlayersEnteredDelaySatisfied(dispatchRequestType) &&
                canProgressToNextScene()
    }

    private fun updateMessages() {
        if (currentGroup != null && this.state == State.HOLDING) {
            lastStatusMessageUpdateTime = System.currentTimeMillis()
            currentGroup!!.cars.forEach { car ->
                car.playerPassengers.forEach { player ->
                    updateMessage(player)
                }
            }
        }
    }

    private fun updateControls() {
        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()
        updateHarnessButton()
    }

    private fun canProgressToNextScene(): Boolean {
        return currentGroup != null && tracklessRide.controller.canContinueToNextSceneIfActionsFinished(currentGroup!!)
    }

    private fun updateRunningIndicator() {
        //        ledRunning.setColor(isRunning() ? ControlColor.GREEN : ControlColor.RED);
    }

    private fun updateDispatchButton() {
        buttonDispatch.isEnabled =
            this.state == State.HOLDING &&
                    !gatesOpen &&
                    !harnessOpen &&
                    canProgressToNextScene() &&
                    canDispatch(DispatchRequestType.OPERATOR)
//        Logger.debug("Enabled ${buttonDispatch.isEnabled}")
    }

    private fun updateGatesButton() {
        buttonGates.isEnabled = this.state == State.HOLDING
        buttonGates.isOn = gatesOpen
    }

    private fun updateHarnessButton() {
        buttonHarness.isEnabled = this.state == State.HOLDING
        buttonHarness.isOn = harnessOpen
    }

    /**
     * @param dispatchRequestType
     * @return true if the station was dispatched
     */
    fun tryDispatchNow(dispatchRequestType: DispatchRequestType): Boolean {
        //        Logger.console("Requesting dispatch %s", dispatchRequestType.name());
//        Logger.debug("Trying dispatch ${dispatchRequestType} ${canDispatch(dispatchRequestType)} ${canProgressToNextScene()}")
        val canDispatch = canDispatch(dispatchRequestType)
        if (canDispatch && canProgressToNextScene()) {
            state = State.DISPATCHING
            return true
        }
        return false
    }

    private fun openGates(open: Boolean) {
        if (this.gatesOpen != open) {
            this.gatesOpen = open
            updateGatesButton()
            updateDispatchButton()

            gateBlocks.forEach { it.open(open) }
        }
    }

    private fun tryOperatorStart() {
//        Logger.debug("Try dispatch ${state} ${gatesOpen}")
        if (this.state == State.HOLDING && !gatesOpen) {
            tryDispatchNow(DispatchRequestType.OPERATOR)
        }
    }

    private fun tryOpenGatesIfPossible(open: Boolean) {
        if (this.state == State.HOLDING) {
            openGates(open)
        }
    }

    private fun openHarness(open: Boolean) {
        if (this.harnessOpen != open) {
            this.harnessOpen = open
            updateHarnessButton()
            updateDispatchButton()
            if (isHarnessSounds)
                playSoundForAllTargetTrainCars(
                    if (open) SoundUtils.RIDE_COASTER_BAR_OPEN else SoundUtils.RIDE_COASTER_BAR_CLOSE,
                    1f,
                    1f
                )
            currentGroup?.cars?.forEach { car ->
                car.canEnter = open && data.isEnter
            }
        }
    }

    private fun tryOpenHarnessIfPossible(open: Boolean) {
        if (this.state == State.HOLDING) {
            openHarness(open)
        }
    }

    private fun playSoundForAllTargetTrainCars(sound: String, volume: Float, pitch: Float) {
        //        Logger.console("Playing %s? %s", sound, targetTrain != null);
        val location = Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0)
        currentGroup?.cars?.forEach { car ->
            val carLocation = car.pathPosition.location
            location.x = carLocation.x
            location.y = carLocation.y
            location.z = carLocation.z

            location.world!!.playSound(location, sound, SoundCategory.MASTER, volume, pitch)
        }
    }

    companion object {
        const val type = "station"
    }

    enum class DispatchRequestType {
        OPERATOR,
        TRAIN_WITH_PLAYERS,
        AUTO,
        AUTO_KEEP_ROLLING,
        DEBUG_ROLLING_TEST
    }

    enum class State {
        IDLE,
        ENTERING,
        HOLDING,
        DISPATCHING
    }

    enum class EjectType {
        TO_SEAT,
        DEFAULT,
    }

    @JsonClass(generateAdapter = true)
    class Data(
        val displayName: String,
        val isEnter: Boolean,
        val gates: Array<Vector>,
        val controlGroupId: String?,
        val ejectLocations: Map<String, List<Location>>?,
        val holdingEjectType: EjectType,
        val autoDispatch: DurationJson? = null,
        val debugInstantStart: Boolean? = null,
        val keepRolling: DurationJson? = null,
        val minimumHold: DurationJson? = null,
        val dispatchInterval: DurationJson? = null,
        val playerEnterAutoDispatchDelay: DurationJson? = null,
    ) : SceneData() {
        override fun toScene(tracklessRide: TracklessRide, id: String): TracklessRideScene =
            TracklessStationScene(id, tracklessRide, this).apply {
                this@Data.autoDispatch?.duration?.let(::setAutoDispatchTime)
                this@Data.debugInstantStart?.let(::setDebugInstantStart)
                this@Data.keepRolling?.duration?.let(::setKeepRollingTime)
                this@Data.minimumHold?.duration?.let(::setMinimumHoldTime)
                this@Data.dispatchInterval?.duration?.let(::setDispatchInterval)
                this@Data.playerEnterAutoDispatchDelay?.duration?.let(::setAutoStartDelay)
            }
    }
}