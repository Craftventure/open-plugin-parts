package net.craftventure.core.ride.trackedride.segment

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.controls.ControlColors
import net.craftventure.core.ride.operator.controls.OperatorButton
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.operator.controls.OperatorSwitch
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.extensions.TransportExtension
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.lang.Double.max
import java.lang.Double.min


class SidewaysTransferSegment(
    id: String,
    trackedRide: TrackedRide,
    transportSpeed: Double,
    accelerateForce: Double,
    maxSpeed: Double,
    brakeForce: Double
) : ExtensibleSegment(
    id,
    id,
    trackedRide
), OperableDependentSegment, OperableTrackSegment, OperatorControl.ControlListener {
    private val transport = TransportExtension(
        transportSpeed,
        accelerateForce,
        maxSpeed,
        brakeForce
    )

    init {
        addExtension(transport)
    }

    var holdingBias = 0.5
    var type: SegmentType = SegmentType.EMBEDDED
    var pullDirection: Direction = Direction.BACKWARDS
    var isPullingIn = false
        private set(value) {
            if (field != value) {
                field = value
                Logger.debug("isPullingIn=$value")

                val sectionEndSegment = sectionEndSegment
                if (value) {
                    if (blockReservedTrain === null) {
                        blockReservedTrain = sectionEndSegment?.blockReservedTrain
                    }
                } else {
                    if (blockReservedTrain === sectionEndSegment?.blockReservedTrain) {
                        blockReservedTrain = anyRideTrainOnSegment
                    }
                }

                pullListeners.forEach { it.invoke(value) }
            }
        }

    val pullListeners = hashSetOf<(Boolean) -> Unit>()

    private var homeIndex = -1
    private val baseOffsetFallback = Vector(0, 0, 0)
    private val baseOffset: Vector
        get() = if (homeIndex > 0) transferTargets[homeIndex].relativeOffset else baseOffsetFallback
    val offset = Vector(0, 0, 0)
    val isHome: Boolean
        get() = targetTransfer == homeIndex

    private val transferTargets = ArrayList<TransferTarget>()
    private var targetTransfer = homeIndex
    var transferEnabled = false
        private set(value) {
            field = value
            clearNextBlock()
            Logger.debug("transferEnabled=$value ${Logger.miniTrace(5)}")
            if (!value)
                isPullingIn = false
            if (!value)
                hasHaltedTrain = false
        }

    var hasHaltedTrain = false
        private set(value) {
            if (field != value) {
                field = value
                Logger.debug("hasHaltedTrain=$value")
                if (value)
                    isPullingIn = false
//                transport.enabled = !value
            }
        }

    var state = State.IDLE
        private set(value) {
            field = value
            setDisableHaltCheck(field != State.IDLE)
            fixTransferTrack()
//            Logger.debug("state=$value")
//            Logger.info("Transfer state is now $value")
        }
    private val updateListeners = HashSet<UpdateListener>()
    private var shouldAddTrain = false
        set(value) {
            if (field != value) {
                field = value
//            Logger.debug("shouldAddTrain=$shouldAddTrain")
                if (value)
                    enableTransfer()
                else
                    disableTransfer()
            }
        }

    private fun clearNextBlock() {
        val train = blockReservedTrain ?: return
        super.getNextTrackSegment()?.clearBlockReservedTrain(train)
    }

    private val isTransferTrackCircuit: Boolean
        get() = targetTransfer == homeIndex && offset.x == 0.0 && offset.y == 0.0 && offset.z == 0.0

    private val isTransferring: Boolean
        get() = !(isTransferTrackCircuit && !transferEnabled)

    private val transferEnabledSwitch =
        OperatorSwitch(getId() + "_transfer_enabled", ControlColors.GREEN, ControlColors.RED).apply {
            owner = this@SidewaysTransferSegment
            name = CVTextColor.MENU_DEFAULT_TITLE + "Transfer mode"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Click to switch transfer mode. Has to be enabled before and while transfering. During transfer mode you can't dispatch trains from the station."
            sort = 1
            group = id
//        permission = Permissions.CREW
            setControlListener(this@SidewaysTransferSegment)
        }

    private val storeButton =
        OperatorButton(getId() + "_store", OperatorButton.Type.DEFAULT).apply {
            owner = this@SidewaysTransferSegment
            name = CVTextColor.MENU_DEFAULT_TITLE + "Store in bay"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Click when the transfer track is in front of a bay to store the train on the transfer track in this bay"
            sort = 2
            group = id
//        permission = Permissions.CREW
//        description = CVChatColor.MENU_DEFAULT_LORE + "Has to be enabled before and while transfering"
            setControlListener(this@SidewaysTransferSegment)
        }

    private val retrieveButton =
        OperatorButton(getId() + "_retrieve", OperatorButton.Type.DEFAULT).apply {
            owner = this@SidewaysTransferSegment
            name = CVTextColor.MENU_DEFAULT_TITLE + "Retrieve from bay"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Click when the transfer track is in front of a bay to retrieve the train from this bay"
            sort = 3
            group = id
//        permission = Permissions.CREW
            setControlListener(this@SidewaysTransferSegment)
        }

    private val nextBayButton =
        OperatorButton(getId() + "_next_bay", OperatorButton.Type.DEFAULT).apply {
            owner = this@SidewaysTransferSegment
            name = CVTextColor.MENU_DEFAULT_TITLE + "Switch to next bay"
            description = CVTextColor.MENU_DEFAULT_LORE + "Moves the transfer track to the next bay"
            sort = 4
            group = id
//        permission = Permissions.CREW
            setControlListener(this@SidewaysTransferSegment)
        }

    private val previousBayButton =
        OperatorButton(getId() + "_previous_bay", OperatorButton.Type.DEFAULT).apply {
            owner = this@SidewaysTransferSegment
            name = CVTextColor.MENU_DEFAULT_TITLE + "Switch to previous bay"
            description = CVTextColor.MENU_DEFAULT_LORE + "Moves the transfer track to the previous bay"
            sort = 5
            group = id
//        permission = Permissions.CREW
            setControlListener(this@SidewaysTransferSegment)
        }

    private val pullInButton =
        OperatorButton(getId() + "_pullIn", OperatorButton.Type.DEFAULT).apply {
            owner = this@SidewaysTransferSegment
            name = CVTextColor.MENU_DEFAULT_TITLE + "Pull in a car"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Requests a train from the section in front of this transfer to be pulled in"
            sort = 6
            group = id
//        permission = Permissions.CREW
            setControlListener(this@SidewaysTransferSegment)
        }

    override fun provideControls(): List<OperatorControl> = listOfNotNull(
        transferEnabledSwitch,
        previousBayButton,
        nextBayButton,
        storeButton,
        retrieveButton,
        if (type == SegmentType.PULL_IN) pullInButton else null
    )

    override fun onClick(
        operableRide: OperableRide,
        player: Player?,
        operatorControl: OperatorControl,
        operatorSlot: Int?
    ) {
        if (!operatorControl.isEnabled) {
            return
        }
//        if (!player.isCrew()) {
//            player.sendMessage(CVChatColor.COMMAND_ERROR + "This feature is only available to crew")
//            return
//        }

        when {
            operatorControl === transferEnabledSwitch -> {
                if (transferEnabled)
                    disableTransfer()
                else
                    enableTransferSafe(player)
            }
            operatorControl === storeButton -> {
                store()
            }
            operatorControl === retrieveButton -> {
                retrieve()
            }
            operatorControl === pullInButton -> {
                isPullingIn = true
            }
            operatorControl === previousBayButton || operatorControl === nextBayButton -> {
                if (anyRideTrainOnSegment?.passengerCount ?: 0 > 0) {
                    player?.sendMessage(CVChatColor.serverError + "Can't switch bay while there are passengers in the train")
                    return
                }
                if (operatorControl === previousBayButton)
                    previousTarget()
                if (operatorControl === nextBayButton)
                    nextTarget()
            }
        }
    }

    override fun debugData(): String? {
        return "shouldAddTrain=$shouldAddTrain " +
                "state=$state " +
                "isTransferring=$isTransferring " +
                "isTransferTrackCircuit=$isTransferTrackCircuit " +
                "transferEnabled=$transferEnabled " +
                "targetTransfer=$targetTransfer " +
                "hasTrain=${this.anyRideTrainOnSegment?.trainId} " +
                "offset=${offset.x},${offset.y},${offset.z} " +
                "baseOffset=${baseOffset.x},${baseOffset.y},${baseOffset.z} " +
                "anyTrainIsBlockedTrain=${anyRideTrainOnSegment != blockReservedTrain} " +
                "anyTrainVelocity=${anyRideTrainOnSegment?.velocity} " +
                "isContainsTrain=$isContainsTrain " +
                "transferContainsTrain=${transferTargets.getOrNull(targetTransfer)?.segment?.isContainsTrain} " +
                "isTrackMoving=${isTrackMoving()} " +
                "transfers=${
                    transferTargets.map { it.segment }
                        .joinToString(",") { "state=${it.state.name} direction=${it.inDirection} targetTrain=${it.targetTrain?.trainId}" }
                }"
    }

    override fun onOperatorsChanged() {
        fixTransferTrack()
    }

    private fun fixTransferTrack() {
        val ride = trackedRide as OperableCoasterTrackedRide
        val operated = ride.isBeingOperated

        shouldAddTrain = false
        if (!operated && trackedRide.ride?.state?.isOpen == true) {
            val segments = transferTargets.map { it.segment }

            val transferTrainCount =
                trackedRide.rideTrains.count { it.frontCarTrackSegment in segments || it.lastCarTrackSegment in segments }

            shouldAddTrain =
                transferTrainCount >= 2 || (state == State.IDLE && this.isContainsTrain/* && this.targetTransfer != this.homeIndex*/)
//            Logger.debug("shouldAddTrain=$shouldAddTrain")
//            shouldAddTrain = true


//            for (train in trackedRide.rideTrains) {
//                if (segments.none { it == train.frontCarTrackSegment } || segments.none { it == train.lastCarTrackSegment }) {
//                    Logger.debug("Cancelling transfer reset: train ${train.trainId} is not on the segment")
//                    shouldAddTrain = false
//                }
//            }
        }
        if (!operated && !shouldAddTrain && state == State.IDLE) {
            targetTransfer = homeIndex
//            Logger.debug("Target transfer set to $homeIndex")
        }

//        Logger.info("Add train@${ride.id}? $shouldAddTrain $targetTransfer $operated")
    }

    fun addUpdateListener(listener: UpdateListener) {
        updateListeners.add(listener)
    }

    fun removeUpdateListener(listener: UpdateListener) {
        updateListeners.remove(listener)
    }

    @JvmOverloads
    fun addTransferTarget(transferTarget: TransferTarget, isHome: Boolean = false) {
        transferTarget.segment.nextTrackSegment = this
        transferTarget.segment.previousTrackSegment = this
        this.transferTargets.add(transferTarget)
        if (isHome) {
            homeIndex = this.transferTargets.size - 1
            if (targetTransfer == -1) {
                targetTransfer = homeIndex
            }
        }
    }

    fun enableTransferSafe(player: Player?) {
        for (rideTrain in trackedRide.rideTrains) {
            if (rideTrain.hasPassengers()) {
                var cancel = false
                val segment = rideTrain.frontCarTrackSegment
                when (segment) {
                    is StationSegment -> {
                        if (segment.state != StationSegment.StationState.HOLDING) {
                            cancel = true
                        }
                    }
                    else -> {
                        cancel = true
                    }
                }
                if (cancel) {
                    player?.sendMessage(CVChatColor.serverError + "Unsafe operation: There appears to be a train that contains guests somewhere on the track")
                    return
                }
            }
        }
        enableTransfer()
    }

    override fun onTrainLeftSection(rideTrain: RideTrain) {
        super.onTrainLeftSection(rideTrain)
        hasHaltedTrain = false
    }

    override fun onTrainEnteredSection(previousSegment: TrackSegment?, rideTrain: RideTrain) {
        super.onTrainEnteredSection(previousSegment, rideTrain)
        hasHaltedTrain = false
        if (blockReservedTrain === null) {
            blockReservedTrain = rideTrain
        }
    }

    fun enableTransfer() {
        if (!transferEnabled) {
            transferEnabled = true
        }
    }

    fun canDisableTransferTrack(): Boolean = isTransferTrackCircuit

    fun disableTransfer(): Boolean {
        return if (canDisableTransferTrack()) {
            if (transferEnabled) {
                transferEnabled = false
                shouldAddTrain = false
            }
            true
        } else {
//            Logger.warn("Couldn't reset transfer track state of")
            false
        }
    }

    fun setTarget(index: Int): Boolean {
        if (!transferEnabled)
            return false
        if (anyRideTrainOnSegment != blockReservedTrain)
            return false
        if (anyRideTrainOnSegment?.velocity ?: 0.0 != 0.0)
            return false

        if (state == State.IDLE) {
            if (index < transferTargets.size) {
                targetTransfer = index
            }
//            Logger.info("Moving to transfer $targetTransfer", true)
            return true
        }
        return false
    }

    fun canSwitchTarget(): Boolean {
        if (!transferEnabled)
            return false
        if (anyRideTrainOnSegment != blockReservedTrain)
            return false
        if (anyRideTrainOnSegment?.velocity ?: 0.0 != 0.0)
            return false

        if (state == State.IDLE) {
            return true
        }
        return false
    }

    fun canSwitchNextTarget(): Boolean {
        return canSwitchTarget() && targetTransfer < transferTargets.size - 1
    }

    fun canSwitchPreviousTarget(): Boolean {
        return canSwitchTarget() && targetTransfer >= 0
    }

    fun previousTarget(): Boolean {
        if (canSwitchPreviousTarget()) {
            if (targetTransfer >= 0) {
                targetTransfer -= 1
            }
//            Logger.info("Moving to transfer $targetTransfer", true)
            return true
        }
        return false
    }

    fun nextTarget(): Boolean {
        if (canSwitchNextTarget()) {
            if (targetTransfer + 1 < transferTargets.size) {
                targetTransfer += 1
            }
//            Logger.info("Moving to transfer $targetTransfer", true)
            return true
        }
        return false
    }

    fun canStore(): Boolean {
        if (!transferEnabled)
            return false
        if (isTrackMoving())
            return false
        if (targetTransfer < 0)
            return false
        if (!isContainsTrain)
            return false
        if (state != State.IDLE)
            return false

        val transfer = transferTargets[targetTransfer]
        if (transfer.segment.isContainsTrain)
            return false

        return true
    }

    fun store(): Boolean {
        if (!canStore())
            return false

        val transfer = transferTargets[targetTransfer]
        state = State.STORING
        transfer.segment.pull()

//        Logger.info("Storing train in transfer $targetTransfer", true)

        return true
    }

    override fun getNextTrackSegment(): TrackSegment {
        val transfer = transferTargets.getOrNull(targetTransfer)
        if (transferEnabled && transfer != null) {
            return transfer.segment
        }
        return super.getNextTrackSegment()!!
    }

    override fun getPreviousTrackSegment(): TrackSegment {
        val transfer = transferTargets.getOrNull(targetTransfer)
        if (transferEnabled && transfer != null) {
            return transfer.segment
        }
        return super.getPreviousTrackSegment()!!
    }

    fun canRetrieve(): Boolean {

        if (!transferEnabled)
            return false
        if (isTrackMoving())
            return false
        if (targetTransfer < 0)
            return false
        if (state != State.IDLE)
            return false
        if (isPullingIn)
            return false
        if (isContainsTrain)
            return false

        val transfer = transferTargets[targetTransfer]
        if (transfer.segment.anyRideTrainOnSegment == null)
            return false

        return true
    }

    fun retrieve(): Boolean {
        if (!canRetrieve())
            return false

        val transfer = transferTargets[targetTransfer]
        state = State.RETRIEVING
        transfer.segment.eject()
        this.blockReservedTrain = transfer.segment.anyRideTrainOnSegment

//        Logger.info("Retrieving train from transfer $targetTransfer", true)

        return true
    }

    fun isTrackMoving(): Boolean {
        val targetOffset = if (targetTransfer < 0) baseOffset else transferTargets[targetTransfer].relativeOffset
        return targetOffset.x != offset.x || targetOffset.y != offset.y || targetOffset.z != offset.z
    }

    override fun reserveBlockForTrain(
        sourceSegment: TrackSegment?,
        previousSegment: TrackSegment?,
        rideTrain: RideTrain?
    ): Boolean {
        if (state != State.IDLE || isTrackMoving() || !isTransferTrackCircuit) {
            return false
        }
        if (super.reserveBlockForTrain(sourceSegment, previousSegment, rideTrain)) {
            if (previousSegment != null)
                this.previousTrackSegment = previousSegment
            return true
        }
        return false
    }

    override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
        if (state != State.IDLE) {
            val transfer = transferTargets[targetTransfer]
            if (state == State.STORING) {
                val shouldMoveForward = transfer.segment.inDirection == Direction.FORWARDS
                car.velocity = if (shouldMoveForward) {
                    BASE_MOVEMENT_VELOCITY
                } else {
                    -BASE_MOVEMENT_VELOCITY
                }
            } else if (state == State.RETRIEVING) {
                val target = transferTargets[targetTransfer]

                val targetDistance = getTargetDistance(car.attachedTrain)
                val applySpeed = when (target.segment.inDirection) {
                    Direction.FORWARDS -> car.attachedTrain.frontCarDistance > targetDistance
                    Direction.BACKWARDS -> car.attachedTrain.frontCarDistance < targetDistance
                }
                if (applySpeed) {
                    car.velocity = when (target.segment.inDirection) {
                        Direction.FORWARDS -> -min(
                            BASE_MOVEMENT_VELOCITY,
                            car.attachedTrain.frontCarDistance - targetDistance
                        )
                        Direction.BACKWARDS -> min(
                            BASE_MOVEMENT_VELOCITY,
                            targetDistance - car.attachedTrain.frontCarDistance
                        )
                    }
                } else {
                    state = State.IDLE
                    car.attachedTrain.cars.forEach { it.velocity = 0.0 }
                }
            }
        } else {
            super.applyForces(car, distanceSinceLastUpdate)

            if (hasHaltedTrain) {
                val targetDistance = getTargetDistance(car.attachedTrain)
                val velocity =
                    (targetDistance - car.attachedTrain.frontCarDistance).clamp(
                        -transport.transportSpeed,
                        transport.transportSpeed
                    )
//                Logger.debug(
//                    "Velocity2 ${velocity.format(2)} for transport=${transport.transportSpeed.format(2)} targetDistance=${targetDistance.format(
//                        2
//                    )} distance=${car.attachedTrain.frontCarDistance.format(
//                        2
//                    )}"
//                )
                car.attachedTrain.cars.forEach {
                    it.velocity = velocity
                    it.acceleration = 0.0
                }
            } else if (transferEnabled) {
                val targetDistance = getTargetDistance(car.attachedTrain)
                if (car.attachedTrain.frontCarDistance + car.attachedTrain.velocity >= targetDistance) {
//                    transport.enabled = false
                    val velocity =
                        (targetDistance - car.attachedTrain.frontCarDistance).clamp(
                            -transport.transportSpeed,
                            transport.transportSpeed
                        )
//                    Logger.debug(
//                        "Velocity ${velocity.format(2)} for transport=${transport.transportSpeed.format(2)} targetDistance=${targetDistance.format(
//                            2
//                        )} distance=${car.attachedTrain.frontCarDistance.format(
//                            2
//                        )}"
//                    )
                    car.attachedTrain.cars.forEach {
                        it.velocity = velocity
                        it.acceleration = 0.0
                    }
//                    if (transferEnabled)
                    hasHaltedTrain = true
                }
            }
        }
    }

    private fun getTargetDistance(train: RideTrain): Double =
        max(length - ((length - train.length) * holdingBias), 0.0)

    override fun update() {
        super.update()

        if (!transferEnabled) {
            hasHaltedTrain = false
        }

        if (shouldAddTrain) {
//            Logger.info("Segment $id for ride ${trackedRide.name} in state=$state with target $targetTransfer moving=${isTrackMoving()}")
            if (state == State.IDLE) {
                val hasTrain = this.anyRideTrainOnSegment != null
                if (!hasTrain) {
                    val target = transferTargets.indexOfFirst { it.segment.anyRideTrainOnSegment != null }
                    if (target < 0) {
                        Logger.severe("Segment has no trains on any targets")
                        shouldAddTrain = false
                    }
                    setTarget(target)

                    if (!isTrackMoving()) {
//                        Logger.info("Now retrieving")
                        retrieve()
                    }
                } else {
                    setTarget(homeIndex)

                    if (!isTrackMoving()) {
                        disableTransfer()
                    }
                }
            }
        }

        val targetOffset = if (targetTransfer < 0) baseOffset else transferTargets[targetTransfer].relativeOffset
        if (offset.x < targetOffset.x) {
            offset.x = offset.x + 0.05

            if (offset.x > targetOffset.x)
                offset.x = targetOffset.x
        } else if (offset.x > targetOffset.x) {
            offset.x = offset.x - 0.05
            if (offset.x < targetOffset.x)
                offset.x = targetOffset.x
        }

        if (offset.y < targetOffset.y) {
            offset.y = offset.y + 0.05

            if (offset.y > targetOffset.y)
                offset.y = targetOffset.y
        } else if (offset.y > targetOffset.y) {
            offset.y = offset.y - 0.05
            if (offset.y < targetOffset.y)
                offset.y = targetOffset.y
        }

        if (offset.z < targetOffset.z) {
            offset.z = offset.z + 0.05

            if (offset.z > targetOffset.z)
                offset.z = targetOffset.z
        } else if (offset.z > targetOffset.z) {
            offset.z = offset.z - 0.05

            if (offset.z < targetOffset.z)
                offset.z = targetOffset.z
        }

        if (state != State.IDLE) {
            val targetTransfer = transferTargets[targetTransfer]
            if (targetTransfer.segment.isIdle()) {
                state = State.IDLE
            }
        }

        if (transferEnabled && !isTrackMoving() && targetTransfer == homeIndex && !trackedRide.isBeingOperated) {
//            Logger.debug("Cancelling transfer operator=${OperatorUtils.getOperatorForSlot(trackedRide, 0)?.name}")
            disableTransfer()
//            transferEnabled = false
        }

        updateListeners.forEach { it.onUpdate(this) }

//        if (isTransferTrackCircuit && previousTrackSegment != originalPreviousSegment && originalPreviousSegment != null) {
//        }

        pullInButton.isEnabled = transferEnabled && !isPullingIn && blockReservedTrain == null && isHome
        storeButton.isEnabled = canStore()
        retrieveButton.isEnabled = canRetrieve()
        transferEnabledSwitch.isOn = transferEnabled
        transferEnabledSwitch.isEnabled = transferEnabled && canDisableTransferTrack() || !transferEnabled
        previousBayButton.isEnabled = canSwitchPreviousTarget()
        nextBayButton.isEnabled = canSwitchNextTarget()
    }

    override fun getPosition(distance: Double, position: Vector, applyInterceptors: Boolean) {
        super.getPosition(distance, position, applyInterceptors)
        position.add(offset)
    }

    class TransferTarget(
        val relativeOffset: Vector,
        val segment: TransferSegment
    )

    class TransferSegment @JvmOverloads constructor(
        id: String,
        trackedRide: TrackedRide,
        val inDirection: Direction = Direction.BACKWARDS,
        var holdingBias: Double = 0.5
    ) : SplinedTrackSegment(
        id,
        id,
        trackedRide
    ) {
        internal var targetTrain: RideTrain? = null
        internal var state = State.IDLE

        fun isIdle() = state == State.IDLE

        fun pull() {
            this.state = State.PULLING
        }

        fun eject() {
            this.state = State.EJECTING
        }

        fun accept(rideTrain: RideTrain): Boolean {
            if (isIdle()) {
                this.targetTrain = rideTrain
                return true
            }
            return false
        }

        override fun debugData(): String? {
            return "inDirection=$inDirection, targetTrain=${targetTrain?.trainId} state=$state"
        }

        private fun getTargetDistance(train: RideTrain): Double =
            max(length - ((length - train.length) * holdingBias), 0.0)

        override fun applyForces(car: RideCar, distanceSinceLastUpdate: Double) {
            if (isIdle()) {
                car.velocity = 0.0
            } else if (state == State.PULLING) {
                val targetDistance = getTargetDistance(car.attachedTrain)

                val applySpeed = when (inDirection) {
                    Direction.FORWARDS -> car.attachedTrain.frontCarDistance < targetDistance
                    Direction.BACKWARDS -> car.attachedTrain.frontCarTrackSegment !== this || car.attachedTrain.frontCarDistance > targetDistance
                }
                if (applySpeed) {
                    car.velocity = when (inDirection) {
                        Direction.FORWARDS -> min(
                            BASE_MOVEMENT_VELOCITY,
                            targetDistance - car.attachedTrain.frontCarDistance
                        )
                        Direction.BACKWARDS -> -min(
                            BASE_MOVEMENT_VELOCITY,
                            if (car.attachedTrain.frontCarTrackSegment === this) car.attachedTrain.frontCarDistance - targetDistance else BASE_MOVEMENT_VELOCITY
                        )
                    }
                } else {
                    state = State.IDLE
                    car.attachedTrain.cars.forEach { it.velocity = 0.0 }
                }
            } else if (state == State.EJECTING) {
                car.velocity = when (inDirection) {
                    Direction.FORWARDS -> -BASE_MOVEMENT_VELOCITY
                    Direction.BACKWARDS -> BASE_MOVEMENT_VELOCITY
                }
            }
        }

        override fun applyForceCheck(car: RideCar?, currentDistance: Double, previousDistance: Double) {
            if (isIdle())
                super.applyForceCheck(car, currentDistance, previousDistance)
        }

        enum class State {
            IDLE, PULLING, EJECTING
        }
    }

    enum class Direction {
        FORWARDS,
        BACKWARDS
    }

    enum class State {
        IDLE,
        STORING,
        RETRIEVING
    }

    enum class SegmentType {
        // Segment is part of the continous loop of the track. It can halt the train
        EMBEDDED,

        // Pull the cart from either the next/previous section
        PULL_IN
    }

    companion object {
        val BASE_MOVEMENT_VELOCITY = CoasterMathUtils.kmhToBpt(4.0)
    }

    interface UpdateListener {
        fun onUpdate(track: SidewaysTransferSegment)
    }
}
