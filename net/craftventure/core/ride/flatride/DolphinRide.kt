package net.craftventure.core.ride.flatride

import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.animation.armature.ArmatureAnimator
import net.craftventure.core.animation.armature.Joint
import net.craftventure.core.animation.dae.DaeLoader
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.forEachAllocationless
import net.craftventure.core.ktx.extension.random
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.ride.RotationFixer
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorAreaTracker
import net.craftventure.core.ride.operator.controls.*
import net.craftventure.core.serverevent.PacketUseEntityEvent
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.core.utils.spawnParticleX
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max


class DolphinRide private constructor() : Flatride<DolphinRide.Arm>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    NAME,
    "ride_$NAME"
), OperableRide, OperatorControl.ControlListener {
    private val npcAreaTracker = NpcAreaTracker(SimpleArea("world", ))
    private val operatorArea = SimpleArea("world", )

    private var rideSettings = RideSettingsState()
        set(value) {
            if (field != value) {
//                Logger.debug("New settings $value")
                field = value
                updateButtons()
            }
        }

    private val entranceGates = arrayOf(
    )
    private val exitGates = arrayOf(
    )
    private var operator: Player? = null
        set(value) {
            if (field !== value) {
                field = value
                if (value == null)
                    rideSettings = presets.random()!!
            }
        }
    private var gatesOpen = false
    override val operatorAreaTracker = OperatorAreaTracker(this, area)
    private val ledRunning = OperatorLed("running_indicator", ControlColors.NEUTRAL_DARK).apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Running"
        description = CVTextColor.MENU_DEFAULT_LORE + "Indicates wether the ride is running or not"
        setControlListener(this@DolphinRide)
    }
    private val buttonDispatch =
        OperatorButton("dispatcher", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Start"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Starts the ride if it's not started yet. Requires gates to be closed"
            setControlListener(this@DolphinRide)
        }
    private val buttonGates =
        OperatorSwitch("gates").apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Gates"
            description = CVTextColor.MENU_DEFAULT_LORE + "Open the gates when the ride is not running"
            setControlListener(this@DolphinRide)
        }

    private val buttonRideSpinSlower =
        OperatorButton("rideSpinSlower", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Ride spin (Slower)"
            group = "a_ridespin"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride slower"
            setControlListener(this@DolphinRide)
        }
    private val buttonRideSpinFaster =
        OperatorButton("rideSpinFaster", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Ride spin (Faster)"
            group = "a_ridespin"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride faster"
            setControlListener(this@DolphinRide)
        }

    private var lastRandomPresetPick = System.currentTimeMillis()
    private val presets = RIDE_SPEEDS.mapIndexed { index, it ->
        RideSettingsState(
            rideSpeed = index
        )
    }
    private val presetColor = ControlColors.BLUE
//    private val presetButtons = presets.map { preset ->
//        OperatorButton(
//            "preset_${preset.name.toLowerCase().replace(" ", "_")}", OperatorButton.Type.DEFAULT,
//            presetColor, presetColor, presetColor
//        ).apply {
//            name = CVChatColor.MENU_DEFAULT_TITLE + "Quick preset: " + preset.name
//            group = "presets"
//            description =
//                CVChatColor.MENU_DEFAULT_LORE + "Quickly switches all settings to the given preset"
//            setControlListener(this@DolphinRide)
//        }
//    }

    private val controls: List<OperatorControl> = listOf(
        buttonDispatch,
        buttonGates,
        buttonRideSpinSlower,
        buttonRideSpinFaster
    )// + presetButtons + reverseButton

    private val animator: ArmatureAnimator

    private var rideRotation = 0.0

    private var rideRotationSpeed = 0.0

    val targetRideRotationSpeed: Double
        get() = RIDE_SPEEDS[rideSettings.rideSpeed]

    init {
        for (entity in area.loc1.world!!.entities) {
            if (entity.name == rideName && area.isInArea(entity.location)) {
                entity.remove()
            }
        }

        val daeFile =
            File(CraftventureCore.getInstance().dataFolder, "data/ride/$NAME/dolphin.dae")
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(daeFile)
        val armatures = DaeLoader.load(doc, "$NAME/ride")
        animator = armatures.map { ArmatureAnimator(it) }.first()
        val armature = animator.armature
        val root = armature.joints.first()
        val arm = root.childJoints.first()

        fun debugJoint(joint: Joint, level: Int = 0) {
            Logger.debug("${"  |".repeat(level)} Joint ${joint.name} ${joint.animatedTransform.toVector3()}")
            val location = joint.animatedTransform.toVector3()
            exitLocation.world.spawnParticleX(
                Particle.END_ROD,
                x = location.x,
                y = location.y,
                z = location.z
            )
            joint.childJoints.forEach { debugJoint(it, level + 1) }
        }

//            Logger.debug("Pre setup")
//            root.childJoints.forEach { debugJoint(it) }

        val matrix = Matrix4x4()
        val quaternion = Quaternion()
//            Logger.debug("Arm=${arm.name} Disc=${disc.name}")
        val armDegrees = 360.0 / ARM_COUNT
        for (i in 1 until ARM_COUNT)
            root.childJoints.add(
                arm.clone().apply { rotateAroundY(this.transform, matrix, quaternion, i * armDegrees) })

        animator.invalidateArmature()
        animator.setTime(0.0)
//        Logger.debug("Armature with Root=${root.animatedTransform.toVector3()}")
//        root.childJoints.forEach { debugJoint(it) }

        cars.clear()
        cars.addAll(root.childJoints.mapIndexed { index, joint -> Arm(joint, index, this) })

        updateButtons()
        updateCarts(true)
        openGates(true)
        npcAreaTracker.startTracking()
    }

    private fun rotateAroundY(
        transform: Matrix4x4,
        calcMatrix: Matrix4x4,
        calcQuat: Quaternion,
        degrees: Double
    ) {
        calcMatrix.setIdentity()
        calcQuat.setIdentity()
        calcMatrix.rotate(calcQuat.rotateAxis(0.0, 1.0, 0.0, degrees))
        calcMatrix.multiply(transform)
        transform.set(calcMatrix)
    }

    override fun onEnter(player: Player?, vehicle: Entity?) {
        super.onEnter(player, vehicle)
        updateCarts(true)
        player?.vehicle?.customName()?.let {
            player.sendMessage(Component.text("You're now riding ", CVTextColor.serverNotice).append(it))
        }
    }

    override fun onLeave(player: Player?, vehicle: Entity?) {
        super.onLeave(player, vehicle)
        updateCarts(true)
    }

    private fun updateButtons() {
        updateRideSpeedButtons()
    }

    private fun updateRideSpeedButtons() {
        buttonRideSpinSlower.displayCount = max(rideSettings.rideSpeed + 1, 1)
        buttonRideSpinSlower.isEnabled = rideSettings.rideSpeed > 0
        buttonRideSpinFaster.displayCount = max(RIDE_SPEEDS.size - rideSettings.rideSpeed, 1)
        buttonRideSpinFaster.isEnabled = rideSettings.rideSpeed < RIDE_SPEEDS.lastIndex
    }

    private fun updateRunningIndicator() {
        ledRunning.color = if (isRunning()) ControlColors.NEUTRAL else ControlColors.NEUTRAL_DARK
        ledRunning.isFlashing = isRunning()
    }

    private fun updateDispatchButton() {
        buttonDispatch.isEnabled = !isRunning() && !gatesOpen
        buttonDispatch.isFlashing = buttonDispatch.isEnabled
    }

    private fun updateGatesButton() {
        buttonGates.isEnabled = !isRunning()
        buttonGates.isOn = gatesOpen
    }

    private fun openGates(open: Boolean) {
        this.gatesOpen = open
        entranceGates.forEach { it.block.open(open) }
        updateGatesButton()
        updateDispatchButton()
    }

    private fun tryOperatorStart() {
        if (!isRunning() && !gatesOpen) {
            start()
        }
    }

    private fun tryOpenGatesIfPossible(open: Boolean) {
        if (!isRunning()) {
            openGates(open)
        }
    }

    override fun start() {
        super.start()
        openGates(false)

        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()

        if (operator == null)
            rideSettings = rideSettings.copy(
                rideSpeed = max(rideSettings.rideSpeed, 1)
            )

        exitGates.forEach { it.block.open(false) }

        pickRandomPreset()
    }

    override fun prepareStart() {
        AudioServerApi.enable("dolphinamuse_onride")
        AudioServerApi.sync("dolphinamuse_onride", System.currentTimeMillis())

        cars.forEach {
            for (armorStand in it.entities) {
                if (armorStand != null && armorStand.passenger is Player) {
                    val player = armorStand.passenger as Player
                    player.sendTitleWithTicks(
                        20,
                        20 * 5,
                        20,
                        NamedTextColor.GOLD,
                        "",
                        NamedTextColor.YELLOW,
                        "Hold space to jump"
                    )
                }
            }
        }
    }

    override fun stop() {
        super.stop()
//        AudioServerApi.disable("swingride")
        if (!isBeingOperated)
            openGates(true)

        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()

        rideRotationSpeed = 0.0

        exitGates.forEach { it.block.open(true) }

        AudioServerApi.disable("dolphinamuse_onride")
        for (i in 0..3)
            updateCarts(true)
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
            operator?.sendMessage(CVChatColor.serverNotice.toString() + "You are now operating " + ride?.displayName)
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
                it.sendMessage(CVChatColor.serverNotice + "You are no longer operating " + ride?.displayName)
                operator = null
                scheduleAutoStart()
                tryOpenGatesIfPossible(true)
            }
        }
    }

    override fun provideControls(): List<OperatorControl> = controls

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
            } else if (operatorControl == buttonGates) {
                tryOpenGatesIfPossible(!buttonGates.isOn)
            } else if (operatorControl == buttonRideSpinSlower) {
                rideSettings = rideSettings.copy(
                    rideSpeed = (rideSettings.rideSpeed - 1).clamp(0, RIDE_SPEEDS.lastIndex)
                )
            } else if (operatorControl == buttonRideSpinFaster) {
                rideSettings = rideSettings.copy(
                    rideSpeed = (rideSettings.rideSpeed + 1).clamp(0, RIDE_SPEEDS.lastIndex)
                )
            }

//            presetButtons.forEachIndexed { index, button ->
//                if (button == operatorControl) {
//                    rideSettings = presets[index]
//                    return
//                }
//            }
        }
    }

    override fun updateCarts(forceTeleport: Boolean) {
//        Logger.debug(" \nUpdating carts ${rotation.format(2)}")
        cars.forEachAllocationless { it.updateTransforms() }
        animator.setTime(0.0)
        cars.forEachAllocationless { it.update() }
    }

    private fun pickRandomPreset() {
        lastRandomPresetPick = System.currentTimeMillis()
        rideSettings = presets.random()!!
    }

    private val shouldPickRandomPreset: Boolean
        get() = operator == null && System.currentTimeMillis() > lastRandomPresetPick + PRESET_PICK_TIME

    override fun provideRunnable(): FlatrideRunnable = RideRunnable()

    fun isStopping() =
        flatrideRunnable?.startTime?.let { System.currentTimeMillis() - it > RIDE_TIME_MILLIS } ?: false

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPacketUseEntityEvent(event: PacketUseEntityEvent) {
//        Logger.info("Received entity use packet event for ${event.interactedEntityId}")
        for (i in cars.indices) {
            val arm = cars[i]
            if (arm.car.model?.entityId == event.interactedEntityId) {
                val seat = arm.car.seats.firstOrNull() ?: return
                val seatEntity = seat.entity ?: return
                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance()) {
                    handleClick(seatEntity, event.player)
                }
            }
        }
    }

    inner class RideRunnable : FlatrideRunnable() {
        override fun updateTick() {
            if (shouldPickRandomPreset) {
                pickRandomPreset()
            }
            val programTime = System.currentTimeMillis() - startTime

            val stopping = programTime > RIDE_TIME_MILLIS

            val deltaRideRotationSpeed = ((if (stopping) 0.0 else targetRideRotationSpeed) - rideRotationSpeed)
                .clamp(
                    -RIDE_ROTATION_ACCELERATION,
                    RIDE_ROTATION_ACCELERATION
                )
            rideRotationSpeed += deltaRideRotationSpeed

            rideRotation += rideRotationSpeed

            if (stopping && rideRotationSpeed == 0.0 && cars.all { it.isSteady }) {
                stop()
//            Logger.debug(
//                "armAngle=${armAngle.format(2)}  armRotation=${armRotation.format(2)}"
//            )
            } else if (stopping && programTime > RIDE_TIME_MILLIS + RIDE_STOP_MILLIS) {
                stop()
            }
            updateCarts(true)
        }
    }

    data class Calculators(
        val matrix: Matrix4x4 = Matrix4x4(),
        val quaternion: Quaternion = Quaternion(),
        val vector: Vector3 = Vector3()
    )

    protected fun applyRotationToEntity(
        jointTransform: Matrix4x4,
        calculators: Calculators,
        entity: NpcEntity,
        rotationFixer: RotationFixer
    ) {
        val position = jointTransform.transformPoint(calculators.vector.reset())
        val headPose = TransformUtils.getArmorStandPose(jointTransform.rotation)
        entity.move(
            position.x,
            position.y - EntityConstants.ArmorStandHeadOffset,
            position.z
        )
//                            entity.customName = "$index ${headPose.x.format(2)}/${headPose.y.format(2)}/${headPose.z.format(2)}"

        rotationFixer.setNextRotation(jointTransform.rotation)
        val rotation = rotationFixer.getCurrentRotation()
//                            entity.headPose = EulerAngle(rotation.x, rotation.y, rotation.z)
        entity.head(rotation.x.toFloat(), rotation.y.toFloat(), rotation.z.toFloat())
    }

    class Arm(val joint: Joint, val armIndex: Int, val ride: DolphinRide) :
        FlatrideCar<ArmorStand>(arrayOfNulls(SEATS_PER_CAR)) {
        val originalTransform = joint.transform.clone()
        val calculators = Calculators()
        val armMoveJoint = joint
        val armModelJoint = armMoveJoint.childJoints.first()
        val car = Car(armModelJoint.childJoints.first(), armIndex, ride = ride, arm = this)
        val relativeYaw = joint.transform.rotation.yawPitchRoll.y.let { if (it < 0) it + 360 else it }
        private val rotationFixer = RotationFixer()
//        private var model: NpcEntity? = null
//            set(value) {
//                if (field != null)
//                    ride.npcAreaTracker.removeEntity(field!!)
//                field = value
//                if (value != null)
//                    ride.npcAreaTracker.addEntity(value)
//            }

        var angle = 0.0
        var velocity = 0.0
        private val staticAngle = 0.0
        private val maxAngle = 45.0
        private val minAngle = -35.0

        override fun isExitAllowed(player: Player, dismounted: Entity): Boolean {
//            Logger.debug("${player.isSneaking} ${player.isInWater()} ${dismounted.isInWater()} ${ride.isRunning()}")
            return player.isSneaking || !ride.isRunning() || ride.isEjecting
        }

        val isSteady: Boolean
            get() = angle == 0.0

        fun updateTransforms() {
            val wantsToJump = car.wantsToJump
            val canJump = (angle >= staticAngle || velocity > 0) && !ride.isStopping() && ride.isRunning()
            val accelerate = wantsToJump && canJump

            if (accelerate) {
                velocity += JUMP_ACCELERATION
                velocity = velocity.clamp(-MAX_VELOCITY, MAX_VELOCITY)
                angle += velocity
                angle = angle.clamp(minAngle, maxAngle)

                if (angle >= maxAngle)
                    velocity = 0.0
            } else {
                if (angle > staticAngle) {
                    velocity -= JUMP_ACCELERATION
                    velocity = velocity.clamp(-MAX_VELOCITY, MAX_VELOCITY)

                    angle += velocity
                    angle = angle.clamp(minAngle, maxAngle)
                }
            }

            if (angle < staticAngle) {
                velocity += JUMP_ACCELERATION
                velocity = velocity.clamp(-MAX_UNDERWATER_VELOCITY, MAX_UNDERWATER_VELOCITY)

                if (angle + velocity >= staticAngle && !accelerate) {
                    angle = staticAngle
                    velocity = 0.0
                } else {
                    angle += velocity
                }
            }

            joint.transform.set(originalTransform)
            ride.rotateAroundY(joint.transform, calculators.matrix, calculators.quaternion, ride.rideRotation)
//            val upAngle = angle
            joint.transform.rotateX(angle)

//            model?.customNameVisible(true)
//            model?.customName(
//                "arm=$armIndex currentRotation=${currentRotation.format(2)}"
//            )

            car.updateTransforms(calculators)
        }

        fun update() {
//            val entity = model ?: NpcEntity(
//                EntityType.ARMOR_STAND, Location(ride.area.world, CENTER.x, CENTER.y, CENTER.z)
//            ).also {
//                model = it
//                it.invisible(true)
////                it.marker(true)
//                it.helmet(MaterialConfig.DOLPHIN_AMUSE_ARM)
//            }
//            ride.applyRotationToEntity(armModelJoint.animatedTransform, calculators, entity, rotationFixer)
//            entity.customNameVisible(true)
//            entity.customName("$armIndex ${ride.rotation.format(2)}")
//            entity.location.spawnParticleX(Particle.FLASH)
//            Logger.debug("Arm=$armIndex ${ride.rotation.format(2)}")
            car.update(calculators)
        }

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            update()
        }
    }

    class Car(
        val joint: Joint,
        val armIndex: Int,
        val ride: DolphinRide,
        val arm: Arm
    ) {
        val originalTransform = joint.transform.clone()
        val seats: Array<Seat> = joint.childJoints
            .mapIndexed { seatIndex, seatJoint -> Seat(seatJoint, armIndex, seatIndex, ride, this) }
            .toTypedArray()

        private val rotationFixer = RotationFixer()
        var model: NpcEntity? = null
            private set(value) {
                if (field != null)
                    ride.npcAreaTracker.removeEntity(field!!)
                field = value
                if (value != null)
                    ride.npcAreaTracker.addEntity(value)
            }
        var hasNameSet = true

        val wantsToJump: Boolean
            get() = seats.any { it.wantsToJump() }

        fun updateTransforms(calculators: Calculators) {
            joint.transform.set(originalTransform)
            joint.transform.rotateZ(arm.angle)
            joint.transform.rotateX((arm.velocity.clamp(-MAX_VELOCITY, MAX_VELOCITY) / MAX_VELOCITY) * -60.0)
            seats.forEach { it.updateTransforms(calculators) }
        }

        fun update(calculators: Calculators) {
            val entity = model ?: NpcEntity(
                "dolphinRide",
                EntityType.ARMOR_STAND, Location(ride.area.world, CENTER.x, CENTER.y, CENTER.z)
            ).also {
                model = it
                it.invisible(true)
                it.helmet(MaterialConfig.DOLPHIN_AMUSE_CAR)
                it.customNameVisible(false)
                it.customName(names[armIndex % names.size])
                hasNameSet = true
            }

            if (ride.isRunning) {
                if (entity.helmet != MaterialConfig.DOLPHIN_AMUSE_CAR_ANIMATED)
                    entity.helmet(MaterialConfig.DOLPHIN_AMUSE_CAR_ANIMATED)
            } else {
                if (entity.helmet != MaterialConfig.DOLPHIN_AMUSE_CAR)
                    entity.helmet(MaterialConfig.DOLPHIN_AMUSE_CAR)
            }

//            if (arm.passengerCount() > 0) {
//                if (!hasNameSet)
//                    entity.customNameVisible(false)
//                hasNameSet = true
//            } else {
//                if (hasNameSet)
////                    entity.customNameVisible(true)
//                hasNameSet = false
//            }
            ride.applyRotationToEntity(joint.animatedTransform, calculators, entity, rotationFixer)

            seats.forEach { it.update(calculators) }
        }
    }

    class Seat(
        val joint: Joint,
        val armIndex: Int,
        val seatIndex: Int,
        val ride: DolphinRide,
        val car: Car
    ) {
        //        private val rotationFixer = RotationFixer()
        private val entityIndex = seatIndex
        private val location = ride.exitLocation.clone()
        private val quaternion = Quaternion()

        val entity: Entity?
            get() = car.arm.entities[entityIndex]

        fun wantsToJump() =
            car.arm.entities[entityIndex]?.passengers?.any { (it as? Player)?.isJumping == true } ?: false

        fun updateTransforms(calculators: Calculators) {
        }

        fun update(calculators: Calculators) {
            val position = joint.animatedTransform.transformPoint(calculators.vector.reset())
//            Logger.debug(
//                "car $armIndex/$seatIndex ${position.x.format(2)} ${position.y.format(2)} ${position.z.format(
//                    2
//                )} >> ${joint.transform.toVector3().x.format(2)} ${joint.transform.toVector3().y.format(2)} ${joint.transform.toVector3().z.format(
//                    2
//                )} >> ${car.joint.animatedTransform.toVector3().x.format(2)} ${car.joint.animatedTransform.toVector3().y.format(
//                    2
//                )} ${car.joint.animatedTransform.toVector3().z.format(2)}"
//            )
            location.set(position.x, position.y, position.z, 0f, 0f)
            val entity = car.arm.entities[entityIndex]?.takeIf { it.isValid } ?: location.spawn<ArmorStand>().also {
                it.isPersistent = false
                it.isInvisible = true
                car.arm.entities[entityIndex] = it
                it.getOrCreateMetadata { TypedInstanceOwnerMetadata(ride = this.ride) }
                it.addDisabledSlots(*EquipmentSlot.values())

                it.isCustomNameVisible = false
                it.customName(names[armIndex % names.size])
            }

            joint.animatedTransform.getRotation(quaternion)
//            entity.customName =
//                "${quaternion.yawPitchRoll.x.format(2)}/${quaternion.yawPitchRoll.y.format(2)}/${quaternion.yawPitchRoll.z.format(
//                    2
//                )}"
//            entity.isCustomNameVisible = true
            EntityUtils.teleport(
                entity,
                position.x,
                position.y - EntityConstants.ArmorStandHeadOffset,
                position.z,
                quaternion.yawPitchRoll.y.toFloat(),
                0f
            )
        }
    }

    companion object {
        const val ARM_COUNT = 8
        const val SEATS_PER_CAR = 1
        const val NAME = "dolphins"
        val CENTER = Vector()
        const val BASE_RADIUS = 14.5
        const val RIDE_TIME_MILLIS = 60_000
        const val RIDE_STOP_MILLIS = 5_000
        const val JUMP_ACCELERATION = 0.095
        const val MAX_VELOCITY = JUMP_ACCELERATION * 20
        const val MAX_UNDERWATER_VELOCITY = JUMP_ACCELERATION * 20

        val names = listOf(
            Component.text("Dorkphine78"),
            Component.text("Aleksandr"),
            Component.text("Red Herring"),
            Component.text("A Dolphin"),
            Component.text("Pistoletov"),
            Component.text("Flipperboy"),
            Component.text("Bubbles"),
            Component.text("Acis"),
        )

        val RIDE_SPEEDS = arrayOf(0.5, 1.5, 2.5)
        const val RIDE_ROTATION_ACCELERATION = 0.04

        val PRESET_PICK_TIME = 15_000

        private var instance: DolphinRide? = null

        fun getInstance(): DolphinRide {
            if (instance == null) {
                instance = DolphinRide()
            }
            return instance!!
        }
    }

    data class RideSettingsState(
        val rideSpeed: Int = 0
    )
}