package net.craftventure.core.ride.trackedride.segment

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.extension.playSound
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.json.DurationJson
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.ride.RideManager
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorManager
import net.craftventure.core.ride.operator.controls.*
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.utils.OperatorUtils
import net.craftventure.core.utils.SimpleInterpolator
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import penner.easing.Linear
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class StationSegment @JvmOverloads constructor(
    id: String,
    displayName: String,
    trackedRide: TrackedRide,
    transportSpeed: Double,
    maxSpeed: Double,
    brakeForce: Double,
    applyLegacyInitFix: Boolean = true
) : SplinedTrackSegment(id, displayName, trackedRide), OperableTrackSegment, OperatorControl.ControlListener {
    private val transportSpeed: Double
    private val maxSpeed: Double
    private val brakeForce: Double

    private var syncTo: StationSegment? = null
    private var syncNodes = hashSetOf<StationSegment>()

    var slowBrakingDistance: Double = 0.0
    var slowBrakingMinSpeed: Double = CoasterMathUtils.kmhToBpt(3.0)
    var slowBrakingInterpolator: SimpleInterpolator =
        SimpleInterpolator { t, b, c, d -> Linear.easeOut(t, b, c, d) }

    var state = StationState.IDLE
        private set(state) {
            if (this.state == state)
                return

            if (dispatchSounds.isNotEmpty()) {
                if (state == StationState.DISPATCHING) {
                    targetTrain?.cars?.firstOrNull()?.location?.let {
                        it.toLocation(trackedRide.area.world)
                    }?.let { location ->
                        location.playSound(dispatchSounds.random(), SoundCategory.AMBIENT, 1f, 1f)
                    }
                }
            }

            if (isBrakeSounds) {
                if (state == StationState.HOLDING)
                    playSoundForAllTargetTrainCars(SoundUtils.RIDE_COASTER_BRAKE_CLOSE, 1f, 1f)
                else if (state == StationState.DISPATCHING)
                    playSoundForAllTargetTrainCars(SoundUtils.RIDE_COASTER_BRAKE_OPEN, 1f, 1f)
            }

            if (state == StationState.HOLDING) {
                if (!OperatorUtils.isBeingOperated(trackedRide)) {
                    openGates(true)
                    openHarness(true)
                }
            } else {
                openGates(false)
                openHarness(false)
            }

            playersEnteredTime = 0
            if (state != StationState.HOLDING) {
                var leavingTrain: RideTrain? = null
                for (rideTrain in trackedRide.getRideTrains()) {
                    if (rideTrain.lastCarTrackSegment === this || rideTrain.frontCarTrackSegment === this) {
                        if (leavingTrain == null)
                            leavingTrain = rideTrain
                        else if (rideTrain.frontCarDistance > leavingTrain.frontCarDistance)
                            leavingTrain = rideTrain
                    }
                }
                if (leavingTrain != null) {
                    for (player in leavingTrain.passengers) {
                        MessageBarManager.remove(player, ChatUtils.ID_RIDE)
                    }
                }
            }
            onStationStateChangeListener.forEach {
                it.onStationStateChanged(state, this.state)
            }
            if (state == StationState.DISPATCHING) {
                lastDepartureTime = System.currentTimeMillis()
            }
            field = state
            lastStateSwitch = System.currentTimeMillis()
            updateControls()
        }
    private var onStationStateChangeListener = hashSetOf<OnStationStateChangeListener>()
    private var onCountdownListener: OnCountdownListener? = null

    var targetTrain: RideTrain? = null
        private set
    var holdDistance: Double = 0.toDouble()
        get() = if (field < 0) getLength() + field else field
    private var lastDepartureTime: Long = 0
    private var minimumHoldTime: Long = 0
    private var dispatchIntervalTime: Long = 0
    private var lastStatusMessageUpdateTime: Long = 0
    private var keepRollingTime: Long = 0

    private var playersEnteredTime: Long = 0
    private var playerEnterAutoDispatchDelay: Long = 10000
    private val decimal2ZeroesFormatter = DecimalFormat("#")

    private val ledClearance: OperatorLed
    private val buttonDispatch: OperatorButton
    private val buttonGates: OperatorSwitch
    private val buttonHarness: OperatorSwitch

    private var dispatchSounds: List<String> = emptyList()
    private var gatesOpen = false
    private var harnessOpen = false
    private var onStationGateListeners = hashSetOf<OnStationGateListener>()

    private var lastAdvanceUpdate = false
    private var lastHarnessClickTime: Long = 0
    private var lastGatesClickTime: Long = 0

    var skipCount: Long = 0
        set(skipCount) {
            field = skipCount
            this.currentSkipCount = skipCount
        }
    private var currentSkipCount: Long = 0
    private var isSkippingTrain = false
    private var debugInstantStart = false
    private var lastStateSwitch: Long = 0
    var accelerateForce: Double? = null
    var isBrakeSounds = true
    var isHarnessSounds = true
    var autoDispatchTime = 0L
        private set
    var queue: RideQueue? = null

    val estimatedMillisecondsUntil: Long?
        get() {
            if (this.state != StationState.HOLDING)
                return null
            var estimate: Long = -1
            if (dispatchIntervalTime > 0) {
                estimate = lastDepartureTime + dispatchIntervalTime - System.currentTimeMillis()
            }

            if (playersEnteredTime > 0) {
                estimate =
                    Math.max(estimate, playersEnteredTime + playerEnterAutoDispatchDelay - System.currentTimeMillis())
            }

            return if (estimate < 0) -1 else estimate
        }

    fun getLastDispatchTime() = lastDepartureTime.takeIf { it != 0L }

    constructor(
        id: String,
        trackedRide: TrackedRide,
        transportSpeed: Double,
        maxSpeed: Double,
        brakeForce: Double
    ) : this(id, id, trackedRide, transportSpeed, maxSpeed, brakeForce)

    init {
        if (applyLegacyInitFix) {
            this.transportSpeed = CoasterMathUtils.kmhToBpt(transportSpeed)
            this.maxSpeed = CoasterMathUtils.kmhToBpt(maxSpeed)
            this.brakeForce = CoasterMathUtils.kmhToBpt(brakeForce)
        } else {
            this.transportSpeed = transportSpeed
            this.maxSpeed = maxSpeed
            this.brakeForce = brakeForce
        }
        blockSection(true)

        ledClearance = OperatorLed(getId() + "_clearance_indicator", ControlColors.RED)
        ledClearance.owner = this
        ledClearance
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Next section free")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "Indicates whether the next section is free so this ride can be dispatched")
            .setSort(1)
            .setGroup(id)
            .setControlListener(this)

        buttonDispatch = OperatorButton(getId() + "_dispatcher", OperatorButton.Type.DEFAULT)
        buttonDispatch.owner = this
        buttonDispatch
            .setFlashing(true)
            .setType(OperatorButton.Type.DISPATCH)
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Dispatch")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "Requires gates to be closed and/or harness to be closed")
            .setSort(2)
            .setGroup(id)
            .setControlListener(this)

        buttonGates = OperatorSwitch(getId() + "_gates")
        buttonGates.owner = this
        buttonGates
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Gates")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "Open/Close the gates when there is a train waiting for dispatch")
            .setSort(3)
            .setGroup(id)
            .setControlListener(this)

        buttonHarness = OperatorSwitch(getId() + "_harness")
        buttonHarness.owner = this
        buttonHarness
            .setType(OperatorSwitch.Type.LOCK_UNLOCK)
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Harness")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "(Un)Lock the harness when there is a train waiting for dispatch")
            .setSort(4)
            .setGroup(id)
            .setControlListener(this)
    }

    fun setAutoDispatchTime(time: Long, unit: TimeUnit) = setAutoDispatchTime(unit.toMillis(time).milliseconds)

    fun setAutoDispatchTime(duration: Duration) {
        this.autoDispatchTime = duration.inWholeMilliseconds
    }

    fun setDebugInstantStart(debugInstantStart: Boolean): StationSegment {
        this.debugInstantStart = debugInstantStart
        return this
    }

    fun setLastDepartureTime(lastDepartureTime: Long) {
        this.lastDepartureTime = lastDepartureTime
    }

    @Deprecated(message = "Use add instead of set")
    fun setOnStationGateListener(action: (open: Boolean) -> Unit) {
        setOnStationGateListener(object : OnStationGateListener {
            override fun onOpenGates(open: Boolean) {
                action(open)
            }
        })
    }

    @Deprecated(message = "Use add instead of set")
    fun setOnStationGateListener(onStationGateListener: OnStationGateListener) {
        onStationGateListeners.clear()
        this.onStationGateListeners.add(onStationGateListener)
    }

    fun addOnStationGateListener(onStationGateListener: OnStationGateListener) {
        this.onStationGateListeners.add(onStationGateListener)
    }


    fun setKeepRollingTime(time: Long, unit: TimeUnit) = setKeepRollingTime(unit.toMillis(time).milliseconds)
    fun setKeepRollingTime(duration: Duration) {
        this.keepRollingTime = duration.inWholeMilliseconds
    }


    fun setMinimumHoldTime(time: Long, unit: TimeUnit) = setMinimumHoldTime(unit.toMillis(time).milliseconds)
    fun setMinimumHoldTime(duration: Duration) {
        this.minimumHoldTime = duration.inWholeMilliseconds
    }


    fun setDispatchIntervalTime(time: Long, unit: TimeUnit) = setDispatchIntervalTime(unit.toMillis(time).milliseconds)
    fun setDispatchIntervalTime(duration: Duration) {
        this.dispatchIntervalTime = duration.inWholeMilliseconds
    }


    fun setAutoStartDelay(time: Long, unit: TimeUnit) = setAutoStartDelay(unit.toMillis(time).milliseconds)
    fun setAutoStartDelay(duration: Duration) {
        this.playerEnterAutoDispatchDelay = duration.inWholeMilliseconds
    }

//    @Deprecated(message="")
//    fun setOnStationStateChangeListener(action: (newState: StationState, oldState: StationState) -> Unit) {
//        this.onStationStateChangeListener.clear()
//        this.onStationStateChangeListener.add(OnStationStateChangeListener { newState, oldState -> action(newState, oldState) })
//    }

    @Deprecated(message = "Use add instead of get")
    fun setOnStationStateChangeListener(onStationStateChangeListener: OnStationStateChangeListener) {
        this.onStationStateChangeListener.clear()
        this.onStationStateChangeListener.add(onStationStateChangeListener)
    }

    fun addOnStationStateChangeListener(onStationStateChangeListener: OnStationStateChangeListener) {
        this.onStationStateChangeListener.add(onStationStateChangeListener)
    }

    fun setOnCountdownListener(onCountdownListener: OnCountdownListener) {
        this.onCountdownListener = onCountdownListener
    }

    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
        super.applyForces(car, distanceSinceLastUpdate)

        if (this.state == StationState.HOLDING) {
            car.acceleration = 0.0
            car.velocity = 0.0
        } else {
            if (accelerateForce != null) {
                val targetSpeed = transportSpeed
                if (targetSpeed > 0) {
                    if (car.velocity + car.acceleration < targetSpeed) {
                        car.acceleration = Math.min(accelerateForce!!, Math.max(targetSpeed - car.velocity, 0.0))
                        //                    Logger.info("Acceleration set to %.2f", false, car.getAcceleration());
                    }
                } else if (targetSpeed < 0) {
                    if (car.velocity + car.acceleration > targetSpeed) {
                        car.acceleration = -Math.min(accelerateForce!!, Math.max(-targetSpeed - car.velocity, 0.0))
                        //                    Logger.info("Acceleration set to %.2f", false, car.getAcceleration());
                    }
                }

                if (maxSpeed != 0.0 && Math.abs(car.velocity) + Math.abs(car.acceleration) > maxSpeed) {
                    if (car.velocity > maxSpeed) {
                        car.acceleration = -Math.min(brakeForce, car.velocity - maxSpeed)
                    } else {
                        car.acceleration = Math.min(brakeForce, maxSpeed - car.velocity)
                    }
                    //                Logger.info("Acceleration corrected to %.2f", false, car.getAcceleration());
                }

            } else {
                if (maxSpeed != 0.0 && car.velocity > maxSpeed) {
                    car.acceleration = -brakeForce
                }

                if (transportSpeed != 0.0 && car.velocity < transportSpeed) {
                    car.acceleration = transportSpeed - car.velocity
                }
            }
        }
    }

    override fun applySecondaryForces(car: RideCar, distanceSinceLastUpdate: Double) {
        super.applySecondaryForces(car, distanceSinceLastUpdate)
        val train = car.attachedTrain
        if (!(slowBrakingDistance > 0 && state == StationState.ENTERING && train.cars[0].trackSegment == this)) return

        val startBraking = holdDistance - slowBrakingDistance
        if (!(train.cars[0].distance > startBraking && train.cars[0].distance < holdDistance)) return

        val distanceProgress = (train.cars[0].distance - startBraking + car.velocity) / (holdDistance - startBraking)
        val progress = 1.0 - slowBrakingInterpolator.interpolate(distanceProgress.clamp(0.0, 1.0), 0.0, 1.0, 1.0)

//        Logger.debug(
//            "A car=${train.cars.indexOf(car)} " +
////                                    "dis=${train.cars[0].distance.format(2)} " +
////                                    "s=${startBraking.format(2)} " +
////                                    "h=${holdDistance.format(2)} " +
////                                    "dP=${distanceProgress.format(2)} " +
//                    "p=${progress.format(2)} " +
//                    "max=${(slowBrakingMinSpeed + ((maxSpeed - slowBrakingMinSpeed) * progress)).format(2)} " +
//                    "vel=${(train.velocity).format(2)}"
//        )

        val appliedMaxSpeed = slowBrakingMinSpeed + ((maxSpeed - slowBrakingMinSpeed) * progress)

        if (appliedMaxSpeed != 0.0 && car.velocity > appliedMaxSpeed) {
            val newAcceleration = -min(brakeForce, car.velocity - appliedMaxSpeed)
            train.cars.forEach {
                it.velocity = car.velocity
                it.acceleration = newAcceleration
            }
//            if (slowBrakingDistance > 0)
//                Logger.debug("E Updated acceleration to ${car.acceleration.format(2)}")
        }
    }

    override fun onPlayerEnteredCarOnSegment(rideCar: RideCar, player: Player) {
        super.onPlayerEnteredCarOnSegment(rideCar, player)

        val passengerCount = rideCar.attachedTrain.passengerCount
        if (passengerCount > 0) return

        //            Logger.console("Setting enter time");
//        Logger.debug("Setting enter time");
        playersEnteredTime = System.currentTimeMillis()
        if (onCountdownListener != null)
            onCountdownListener!!.onCountdownStarted()
        //        Logger.console("Entered, passengers %s", passengerCount);
    }

    override fun onPlayerExitedCarOnSegment(rideCar: RideCar, player: Player) {
        super.onPlayerExitedCarOnSegment(rideCar, player)
        MessageBarManager.remove(player, ChatUtils.ID_RIDE)
        //        Logger.console("Leaving, passengers %s", passengerCount);
    }

    private fun updateMessage(player: Player) {
        if (this.state != StationState.HOLDING) {
            return
        }

        val rideName = if (trackedRide.ride != null) trackedRide.ride!!.displayName else trackedRide.name
        if (OperatorUtils.isBeingOperated(trackedRide)) {
            val operator = OperatorUtils.getOperatorForSlot(trackedRide, 0)
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
                if (syncTo != null || syncNodes.isNotEmpty()) {
                    display(
                        player,
                        Message(
                            id = ChatUtils.ID_RIDE,
                            text = Translation.RIDE_WAITING_SYNC_STATION.getTranslation(
                                player,
                                rideName
                            )!!,
                            type = MessageBarManager.Type.RIDE,
                            untilMillis = TimeUtils.secondsFromNow(1.0),
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
    }

    private fun updateMessages() {
        if (targetTrain != null && this.state == StationState.HOLDING) {
            lastStatusMessageUpdateTime = System.currentTimeMillis()
            for (player in targetTrain!!.passengers) {
                updateMessage(player)
            }
        }
    }

    private fun shouldSkipNextTrain(): Boolean {
        return skipCount > 0 && currentSkipCount < skipCount
    }

    private fun checkTrainSkipCount() {
        val containsTrain = isContainsTrainCached
        if (containsTrain && this.state == StationState.IDLE) {
            if (skipCount > 0 && currentSkipCount < skipCount) {
                isSkippingTrain = true
            } else {
                state = StationState.ENTERING
                currentSkipCount = 0
                isSkippingTrain = false
            }
        }
        if (!containsTrain && isSkippingTrain) {
            isSkippingTrain = false
            currentSkipCount++
        }
    }

    override fun update() {
        super.update()
        checkTrainSkipCount()

        if (this.state == StationState.HOLDING) {
            //            boolean isBeingOperated = OperatorUtils.isBeingOperated(getFlatride());
            if (targetTrain != null && !targetTrain!!.canEnter()) {
                targetTrain!!.setCanEnter(harnessOpen)
            }

            if (playersEnteredTime > 0) {
                tryDispatchNow(DispatchRequestType.AUTO)
            } else if (trackedRide.hasPassengersInNonEnterableTrains()) {
                tryDispatchNow(DispatchRequestType.AUTO_KEEP_ROLLING)
            } else if (debugInstantStart) {
                tryDispatchNow(DispatchRequestType.DEBUG_ROLLING_TEST)
            }
        } else {
            if (targetTrain != null && targetTrain!!.canEnter()) {
                targetTrain!!.setCanEnter(false)
            }

            if (this.state != StationState.HOLDING && this.state != StationState.DISPATCHING) {
                if (targetTrain != null && !isSkippingTrain) {
                    if (targetTrain!!.frontCarDistance >= holdDistance) {//+ holdDistance > getLength() - ((getLength() - targetTrain.getLength()) / 2.0)) {
                        state = StationState.HOLDING
                        targetTrain!!.halt()
                        targetTrain!!.eject()
                    }
                }
            }
        }

        if (lastStatusMessageUpdateTime < System.currentTimeMillis() - 500) {
            updateMessages()
        }


        //        isWaitMinimumTimeInstationSatisfied(dispatchRequestType) &&
        //                isKeepRollingSatisfied(dispatchRequestType) &&
        //                isMinimumHoldTimeSatisfied(dispatchRequestType) &&
        //                isPlayersEnteredDelaySatisfied(dispatchRequestType) &&
        //                hasClearanceOnNextSection()
        val advanceUpdate = hasClearanceOnNextSection() && canTargetTrainAdvance() &&
                isWaitMinimumTimeInstationSatisfied(DispatchRequestType.OPERATOR) &&
                isKeepRollingSatisfied(DispatchRequestType.OPERATOR) &&
                isMinimumHoldTimeSatisfied(DispatchRequestType.OPERATOR) &&
                isDispatchIntervalTimeSatisfied(DispatchRequestType.OPERATOR) &&
                isPlayersEnteredDelaySatisfied(DispatchRequestType.OPERATOR)
        if (advanceUpdate != lastAdvanceUpdate) {
            lastAdvanceUpdate = advanceUpdate
//            ledClearance.color = if (advanceUpdate) ControlColors.GREEN else ControlColors.RED
            ledClearance.color = if (advanceUpdate) ControlColors.NEUTRAL else ControlColors.NEUTRAL_DARK
            ledClearance.isFlashing = advanceUpdate
        }
        if (RideManager.allowAutoDispatch && autoDispatchTime > 0 && autoDispatchTime < System.currentTimeMillis() - lastStateSwitch) {
            if (isDispatchIntervalTimeSatisfied(DispatchRequestType.AUTO)) {
                tryDispatchNow(DispatchRequestType.AUTO)
            }
        }
        updateDispatchButton()

        val targetTrain = targetTrain
        if (targetTrain != null) {
            val passengerCount = targetTrain.passengerCount
            if (passengerCount == 0 && playersEnteredTime != 0L) {
//                Logger.debug("Resetting timer")
                playersEnteredTime = 0
                if (onCountdownListener != null)
                    onCountdownListener!!.onCountdownStopped()
                //            Logger.console("Resetting enter time");
            }
        }
    }

    override fun getLeaveLocation(player: Player, rideCar: RideCar, leaveType: LeaveType): Location {
        return if (queue != null && queue!!.isActive) {
            trackedRide.exitLocation
        } else super.getLeaveLocation(player, rideCar, leaveType)
    }

    override fun canLeaveSection(rideTrain: RideTrain): Boolean {
        if (isSkippingTrain)
            return true
        return if (this.state != StationState.DISPATCHING) {
            false
        } else super.canLeaveSection(rideTrain)
    }

    private fun canTargetTrainAdvance(): Boolean {
        return this.state == StationState.HOLDING && targetTrain != null &&
                canAdvanceToNextBlock(targetTrain!!, false) || nextTrackSegment!!.isSectionUnreserved(this)
    }

    override fun onTrainLeftSection(rideTrain: RideTrain?) {
        super.onTrainLeftSection(rideTrain)
        if (rideTrain === targetTrain) {
            if (this.state != StationState.DISPATCHING) {
                Logger.severe(
                    "A train left segment %s with state %s at ride %s while the station didn't allow the train to leave. BlockSectionSystem reports to be valid",
                    true,
                    id, this.state, trackedRide.name
                )
            }
            state = StationState.IDLE
            targetTrain = null
        } else {
        }
    }

    override fun onTrainEnteredSection(previousSegment: TrackSegment?, rideTrain: RideTrain) {
        super.onTrainEnteredSection(previousSegment, rideTrain)
        if (targetTrain == null) {
            checkTrainSkipCount()
            if (!shouldSkipNextTrain()) {
                //                Logger.info("Setting target train for skipping = false");
                targetTrain = rideTrain
            }
            //            if (state == StationState.IDLE) {
            //                setState(StationState.ENTERING);
            //            }
        } else {
        }
    }

    override fun reserveBlockForTrain(
        sourceSegment: TrackSegment,
        previousSegment: TrackSegment,
        rideTrain: RideTrain
    ): Boolean {
        if (this.state == StationState.HOLDING && !OperatorUtils.isBeingOperated(trackedRide) && rideTrain.hasPassengers()) {
            tryDispatchNow(DispatchRequestType.TRAIN_WITH_PLAYERS)
        }
        return super.reserveBlockForTrain(sourceSegment, previousSegment, rideTrain)
    }

    //    @Override
    //    public boolean canAdvanceToNextBlock(@Nonnull RideTrain rideTrain, boolean reserveNextBlockIfPossible) {
    //        if (reserveNextBlockIfPossible && !OperatorUtils.isBeingOperated(getFlatride()) && rideTrain.hasPassengers()) {
    //            tryDispatchNow(DispatchRequestType.TRAIN_WITH_PLAYERS);
    //        }
    //        return super.canAdvanceToNextBlock(rideTrain, reserveNextBlockIfPossible);
    //    }

    private fun isWaitMinimumTimeInstationSatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return lastStateSwitch < System.currentTimeMillis() - 5000 || dispatchRequestType == DispatchRequestType.OPERATOR
    }

    private fun isMinimumHoldTimeSatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return lastStateSwitch < System.currentTimeMillis() - minimumHoldTime || dispatchRequestType == DispatchRequestType.OPERATOR
    }

    private fun isDispatchIntervalTimeSatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return dispatchIntervalTime <= 0 || lastDepartureTime < System.currentTimeMillis() - dispatchIntervalTime
    }

    private fun isPlayersEnteredDelaySatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return playersEnteredTime <= 0 || playersEnteredTime < System.currentTimeMillis() - playerEnterAutoDispatchDelay || dispatchRequestType == DispatchRequestType.OPERATOR
    }

    private fun hasClearanceOnNextSection(): Boolean {
        return targetTrain != null && (canAdvanceToNextBlock(
            targetTrain!!,
            false
        ) || nextTrackSegment!!.isSectionUnreserved(this))
    }

    private fun isKeepRollingSatisfied(dispatchRequestType: DispatchRequestType): Boolean {
        return if (dispatchRequestType == DispatchRequestType.AUTO_KEEP_ROLLING) {
            keepRollingTime > 0 && lastDepartureTime < System.currentTimeMillis() - keepRollingTime
        } else true
    }

    fun canDispatch(dispatchRequestType: DispatchRequestType): Boolean {
        if (syncTo != null && dispatchRequestType != DispatchRequestType.SYNC_DISPATCH) return false
        if (targetTrain == null) {
            return false
        }
        if (trackedRide.isEmergencyStopActive) return false
        if (syncTo == null) {
            val isBeingOperated = OperatorUtils.isBeingOperated(trackedRide)
            if (dispatchRequestType != DispatchRequestType.OPERATOR && isBeingOperated) {
                return false
            } else if (dispatchRequestType == DispatchRequestType.OPERATOR && !isBeingOperated) {
                return false
            }
        }
        if (trackedRide.passengerCount > 0)
            for (trackSegment in trackedRide.getTrackSegments()) {
                if (trackSegment is SidewaysTransferSegment) {
                    if (trackSegment.transferEnabled) {
                        return false
                    }
                }
            }
        if (syncNodes.isNotEmpty() && !syncNodes.all { it.canDispatch(DispatchRequestType.SYNC_DISPATCH) }) {
            return false
        }
        return this.state == StationState.HOLDING &&
                isWaitMinimumTimeInstationSatisfied(dispatchRequestType) &&
                isKeepRollingSatisfied(dispatchRequestType) &&
                isMinimumHoldTimeSatisfied(dispatchRequestType) &&
                isDispatchIntervalTimeSatisfied(dispatchRequestType) &&
                isPlayersEnteredDelaySatisfied(dispatchRequestType) &&
                hasClearanceOnNextSection() &&
                canAdvanceToNextBlock(targetTrain!!, false, true)
    }

    /**
     * @param dispatchRequestType
     * @return true if the station was dispatched
     */
    fun tryDispatchNow(dispatchRequestType: DispatchRequestType): Boolean {
        if (syncTo != null) {
            if (dispatchRequestType != DispatchRequestType.SYNC_DISPATCH) {
                return syncTo!!.tryDispatchNow(dispatchRequestType)
            }
        }
        //        Logger.console("Requesting dispatch %s", dispatchRequestType.name());
        val canDispatch = canDispatch(dispatchRequestType)
        if (canDispatch && canAdvanceToNextBlock(targetTrain!!, true)) {
            state = StationState.DISPATCHING
            syncNodes.forEach {
                if (!it.tryDispatchNow(DispatchRequestType.SYNC_DISPATCH)) {
                    Logger.debug(
                        "Tried to dispatch station ${it.id} of ${it.trackedRide.name} by primary $id but failed (this should be impossible?)",
                        logToCrew = true
                    )
                }
            }
            return true
        }
        return false
    }

    fun clearNodes() {
        syncNodes.forEach {
            it.syncTo = null
        }
        syncNodes.clear()
    }

    fun addSyncNode(segment: StationSegment) {
        if (syncNodes.add(segment)) {
            segment.syncTo = this
        }
    }

    override fun provideControls(): List<OperatorControl> {
        val controls = ArrayList<OperatorControl>()
        controls.add(ledClearance)
        controls.add(buttonDispatch)
        controls.add(buttonGates)
        controls.add(buttonHarness)
        return controls
    }

    override fun onOperatorsChanged() {
        if (!OperatorUtils.isBeingOperated(trackedRide)) {
            tryOpenHarnessIfPossible(true)
            tryOpenGatesIfPossible(true)
        }
    }

    override fun onClick(
        operableRide: OperableRide,
        player: Player?,
        operatorControl: OperatorControl,
        operatorSlot: Int?
    ) {
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

    private fun updateControls() {
        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()
        updateHarnessButton()
    }

    private fun updateRunningIndicator() {
        //        ledRunning.setColor(isRunning() ? ControlColor.GREEN : ControlColor.RED);
    }

    private fun updateDispatchButton() {
        buttonDispatch.isEnabled =
            this.state == StationState.HOLDING && !gatesOpen && !harnessOpen && canTargetTrainAdvance() && canDispatch(
                DispatchRequestType.OPERATOR
            )
    }

    private fun updateGatesButton() {
        buttonGates.isEnabled = this.state == StationState.HOLDING
        buttonGates.isOn = gatesOpen
    }

    private fun updateHarnessButton() {
        buttonHarness.isEnabled = this.state == StationState.HOLDING
        buttonHarness.isOn = harnessOpen
    }

    private fun openGates(open: Boolean) {
        if (this.gatesOpen != open) {
            this.gatesOpen = open
            updateGatesButton()
            updateDispatchButton()

            onStationGateListeners.forEach {
                it.onOpenGates(open)
            }
        }
    }

    private fun tryOperatorStart() {
        if (this.state == StationState.HOLDING && !gatesOpen) {
            tryDispatchNow(DispatchRequestType.OPERATOR)
        }
    }

    private fun tryOpenGatesIfPossible(open: Boolean) {
        if (this.state == StationState.HOLDING) {
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
            if (targetTrain != null) {
                targetTrain!!.setCanEnter(open)
            }
        }
    }

    private fun tryOpenHarnessIfPossible(open: Boolean) {
        if (this.state == StationState.HOLDING) {
            openHarness(open)
        }
    }

    private fun diffWithNowString(other: Long): String {
        return if (other == 0L) String.format("%d", other) else String.format(
            "%d (diff=%d)",
            other,
            System.currentTimeMillis() - other
        )
    }

    override fun debugData(): String? {
        return String.format(
            "lastDepartureTime=%s minimumHoldTime=%s lastStatusMessageUpdateTime=%s keepRollingTime=%s playersEnteredTime=%s playerEnterAutoDispatchDelay=%s state=%s gatesOpen=%s harnessOpen=%s lastAdvanceUpdate=%s lastHarnessClickTime=%s lastGatesClickTime=%s skipCount=%s currentSkipCount=%s isSkippingTrain=%s debugInstantStart=%s lastStateSwitch=%s autoDispatchTime=%s now=%s",
            diffWithNowString(lastDepartureTime),
            dispatchIntervalTime,
            diffWithNowString(lastStatusMessageUpdateTime),
            keepRollingTime,
            diffWithNowString(playersEnteredTime),
            playerEnterAutoDispatchDelay,
            this.state,
            gatesOpen,
            harnessOpen,
            lastAdvanceUpdate,
            diffWithNowString(lastHarnessClickTime),
            diffWithNowString(lastGatesClickTime),
            this.skipCount,
            currentSkipCount,
            isSkippingTrain,
            debugInstantStart,
            diffWithNowString(lastStateSwitch),
            autoDispatchTime,
            System.currentTimeMillis()
        )
    }

    private fun playSoundForAllTargetTrainCars(sound: String, volume: Float, pitch: Float) {
        //        Logger.console("Playing %s? %s", sound, targetTrain != null);
        if (targetTrain != null) {
            val location = Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0)
            for (rideCar in targetTrain!!.cars) {
                val carLocation = rideCar.location
                location.x = carLocation.x
                location.y = carLocation.y
                location.z = carLocation.z

                location.world!!.playSound(location, sound, SoundCategory.MASTER, volume, pitch)
            }
        }
    }

    override fun toJson(): Json {
        val json = Json()
        return toJson(json)
    }

    override fun <T : TrackSegmentJson?> toJson(source: T): T & Any {
        source as Json

        source.accelerateForce = accelerateForce
        source.holdDistance = holdDistance
        source.slowBrakingDistance = slowBrakingDistance
        source.slowBrakingMinSpeed = slowBrakingMinSpeed

        source.transportSpeed = transportSpeed
        source.maxSpeed = maxSpeed
        source.brakeForce = brakeForce

        source.isBrakeSounds = isBrakeSounds
        source.isHarnessSounds = isHarnessSounds
        source.autoDispatchTime = DurationJson(autoDispatchTime.toDuration(DurationUnit.MILLISECONDS))

        source.dispatchIntervalTime = DurationJson(dispatchIntervalTime.toDuration(DurationUnit.MILLISECONDS))
        source.keepRollingTime = DurationJson(keepRollingTime.toDuration(DurationUnit.MILLISECONDS))
        source.playerEnterAutoDispatchDelay =
            DurationJson(playerEnterAutoDispatchDelay.toDuration(DurationUnit.MILLISECONDS))
        source.minimumHoldTime = DurationJson(minimumHoldTime.toDuration(DurationUnit.MILLISECONDS))
        source.dispatchSounds = dispatchSounds

        return super.toJson(source)
    }

    override fun <T : TrackSegmentJson?> restore(source: T) {
        source as Json
        slowBrakingDistance = source.slowBrakingDistance
        slowBrakingMinSpeed = source.slowBrakingMinSpeed
        accelerateForce = source.accelerateForce
        holdDistance = source.holdDistance
//        transportSpeed = source.transportSpeed
//        maxSpeed = source.maxSpeed
//        brakeForce = source.brakeForce
        isBrakeSounds = source.isBrakeSounds
        isHarnessSounds = source.isHarnessSounds
        autoDispatchTime = source.autoDispatchTime.inWholeMilliseconds
        dispatchIntervalTime = source.dispatchIntervalTime.inWholeMilliseconds
        keepRollingTime = source.keepRollingTime.inWholeMilliseconds
        playerEnterAutoDispatchDelay = source.playerEnterAutoDispatchDelay.inWholeMilliseconds
        minimumHoldTime = source.minimumHoldTime.inWholeMilliseconds
        dispatchSounds = source.dispatchSounds

        super.restore(source)
    }

    enum class DispatchRequestType {
        OPERATOR,
        TRAIN_WITH_PLAYERS,
        AUTO,
        AUTO_KEEP_ROLLING,
        DEBUG_ROLLING_TEST,
        SYNC_DISPATCH,
    }

    enum class StationState {
        IDLE,
        ENTERING,
        HOLDING,
        DISPATCHING
    }

    fun interface OnStationGateListener {
        fun onOpenGates(open: Boolean)
    }

    fun interface OnStationStateChangeListener {
        fun onStationStateChanged(newState: StationState, oldState: StationState)
    }

    interface OnCountdownListener {
        fun onCountdownStarted()

        fun onCountdownStopped()
    }

    @JsonClass(generateAdapter = true)
    open class Json : SplinedTrackSegmentJson() {
        var slowBrakingDistance: Double = 0.0
        var slowBrakingMinSpeed: Double = 3.0

        var accelerateForce: Double? = null
        var holdDistance: Double = 0.0
        var transportSpeed: Double = 0.0
        var maxSpeed: Double = 0.0
        var brakeForce: Double = 0.0

        var dispatchSounds: List<String> = emptyList()
        var isBrakeSounds = true
        var isHarnessSounds = true
        var autoDispatchTime: DurationJson = DurationJson(0.toDuration(DurationUnit.SECONDS))

        var minimumHoldTime: DurationJson = DurationJson(0.toDuration(DurationUnit.SECONDS))
        var dispatchIntervalTime: DurationJson = DurationJson(0.toDuration(DurationUnit.SECONDS))
        var keepRollingTime: DurationJson = DurationJson(0.toDuration(DurationUnit.SECONDS))
        var playerEnterAutoDispatchDelay: DurationJson = DurationJson(10.toDuration(DurationUnit.SECONDS))

        var gates: Set<Location>? = null
        var soundtracks: Set<String>? = null

        override fun create(trackedRide: TrackedRide): TrackSegment =
            StationSegment(
                id,
                displayName,
                trackedRide,
                transportSpeed,
                maxSpeed,
                brakeForce,
                applyLegacyInitFix = false
            ).apply {
                this.restore(this@Json)

                val gates = this@Json.gates
                if (gates != null) {
                    addOnStationGateListener(object : OnStationGateListener {
                        override fun onOpenGates(open: Boolean) {
                            gates.forEach {
                                it.block.open(open)
                            }
                        }

                    })
                }

                val soundtracks = soundtracks
                if (soundtracks != null && soundtracks.isNotEmpty()) {
                    setOnStationStateChangeListener({ newState: StationState, oldState: StationState? ->
                        if (newState === StationState.DISPATCHING) {
                            val rideTrain = anyRideTrainOnSegment
                            if (rideTrain != null) {
                                rideTrain.setOnboardSynchronizedAudio(soundtracks.random(), System.currentTimeMillis())
                            }
                        }
                    })
                }
            }
    }
}
