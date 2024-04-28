package net.craftventure.core.ride.flatride

import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.chat.bungee.extension.plus
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
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.ride.RotationFixer
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorAreaTracker
import net.craftventure.core.ride.operator.controls.*
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max
import kotlin.math.sin


class Rocktopus private constructor() : Flatride<Rocktopus.Arm>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    NAME,
    "ride_$NAME"
), OperableRide, OperatorControl.ControlListener {
    private val npcAreaTracker = NpcAreaTracker(SimpleArea("world",))
    private val operatorArea = SimpleArea("world",)

    private var rideSettings = RideSettingsState()
        set(value) {
            if (field != value) {
//                Logger.debug("New settings $value")
                field = value
                updateButtons()
            }
        }

    private val entranceGates = arrayOf(
        Location(Bukkit.getWorld("world"),),
        Location(Bukkit.getWorld("world"),)
    )
    private val exitGates = arrayOf(
        Location(Bukkit.getWorld("world"),)
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
        setControlListener(this@Rocktopus)
    }
    private val buttonDispatch =
        OperatorButton("dispatcher", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Start"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Starts the ride if it's not started yet. Requires gates to be closed"
            setControlListener(this@Rocktopus)
        }
    private val buttonGates =
        OperatorSwitch("gates").apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Gates"
            description = CVTextColor.MENU_DEFAULT_LORE + "Open the gates when the ride is not running"
            setControlListener(this@Rocktopus)
        }

    private val buttonRideSpinDirection =
        OperatorSwitch("rideSpinForwards").apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Ride spin direction"
            type = OperatorSwitch.Type.FORWARDS_BACKWARDS
            group = "a_ridespin"
            groupDisplayName = "Ride (spin)"
            isOn = true
            description = CVTextColor.MENU_DEFAULT_LORE + "Set the ride spin direction"
            setControlListener(this@Rocktopus)
        }
    private val buttonRideSpinSlower =
        OperatorButton("rideSpinSlower", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Ride spin (Slower)"
            group = "a_ridespin"
            groupDisplayName = "Ride (spin)"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride slower"
            setControlListener(this@Rocktopus)
        }
    private val buttonRideSpinFaster =
        OperatorButton("rideSpinFaster", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Ride spin (Faster)"
            group = "a_ridespin"
            groupDisplayName = "Ride (spin)"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride faster"
            setControlListener(this@Rocktopus)
        }

    private val buttonArmSpinDirection =
        OperatorSwitch("armSpinForwards").apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Arm spin direction"
            type = OperatorSwitch.Type.FORWARDS_BACKWARDS
            group = "b_arm"
            groupDisplayName = "Arm"
            isOn = true
            description = CVTextColor.MENU_DEFAULT_LORE + "Set the arm spin direction"
            setControlListener(this@Rocktopus)
        }
    private val buttonArmSpinSlower =
        OperatorButton("armSpinSlower", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Arm spin (Slower)"
            group = "b_arm"
            groupDisplayName = "Arm"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the arm slower"
            setControlListener(this@Rocktopus)
        }
    private val buttonArmSpinFaster =
        OperatorButton("armSpinFaster", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Arm spin (Faster)"
            group = "b_arm"
            groupDisplayName = "Arm"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the arm faster"
            setControlListener(this@Rocktopus)
        }

    private val buttonDiscSpinDirection =
        OperatorSwitch("discSpinForwards").apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Disc spin direction"
            type = OperatorSwitch.Type.FORWARDS_BACKWARDS
            group = "c_disc"
            groupDisplayName = "Disc"
            isOn = true
            description = CVTextColor.MENU_DEFAULT_LORE + "Set the disc spin direction"
            setControlListener(this@Rocktopus)
        }
    private val buttonDiscSpinSlower =
        OperatorButton("discSpinSlower", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Disc spin (Slower)"
            group = "c_disc"
            groupDisplayName = "Disc"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride slower"
            setControlListener(this@Rocktopus)
        }
    private val buttonDiscSpinFaster =
        OperatorButton("discSpinFaster", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Disc spin (Faster)"
            group = "c_disc"
            groupDisplayName = "Disc"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride faster"
            setControlListener(this@Rocktopus)
        }

    private val buttonArmLower =
        OperatorButton("armLower", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Arms (lower)"
            group = "a_arms"
            groupDisplayName = "Arms"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride slower"
            setControlListener(this@Rocktopus)
        }
    private val buttonArmHigher =
        OperatorButton("armHigher", OperatorButton.Type.DEFAULT).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + "Arms (higher)"
            group = "a_arms"
            groupDisplayName = "Arms"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Spin the ride faster"
            setControlListener(this@Rocktopus)
        }

    private var lastRandomPresetPick = System.currentTimeMillis()
    private val presets = listOf(
        RideSettingsState(
            name = "Waltz",
            rideSpeed = 1,
            armSpeed = 1,
            discSpeed = 2,
            armDegrees = ARM_DEGREES.lastIndex
        ),
        RideSettingsState(
            name = "Turbo Waltz",
            rideSpeed = 2,
            armSpeed = ARM_SPEEDS.lastIndex,
            discSpeed = 2,
            armDegrees = ARM_DEGREES.lastIndex
        ),
        RideSettingsState(
            name = "Ground spinner",
            rideSpeed = 1,
            armSpeed = 0,
            discSpeed = 2,
            armDegrees = 0
        ),
//        RideSettingsState(
//            name = "Ground spinner deluxe",
//            rideSpeed = RIDE_SPEEDS.lastIndex,
//            armSpeed = 0,
//            discSpeed = DISC_SPEEDS.lastIndex,
//            armDegrees = 0
//        ),
        RideSettingsState(
            name = "Turbo! Turbo! Turbo!",
            rideSpeed = RIDE_SPEEDS.lastIndex,
            armSpeed = ARM_SPEEDS.lastIndex,
            discSpeed = DISC_SPEEDS.lastIndex,
            armDegrees = ARM_DEGREES.lastIndex
        )
    )
    private val presetColor = ControlColors.BLUE
    private val presetButtons = presets.map { preset ->
        OperatorButton(
            "preset_${preset.name.lowercase(Locale.getDefault()).replace(" ", "_")}", OperatorButton.Type.DEFAULT,
            presetColor, presetColor, presetColor
        ).apply {
            name = CVTextColor.MENU_DEFAULT_TITLE + ("Quick preset: " + preset.name)
            group = "presets"
            description =
                CVTextColor.MENU_DEFAULT_LORE + "Quickly switches all settings to the given preset"
            setControlListener(this@Rocktopus)
        }
    }
    private val reverseButton = OperatorButton(
        "preset_reverse", OperatorButton.Type.DEFAULT,
        presetColor, presetColor, presetColor
    ).apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Quick preset: Reverse rotations"
        group = "presets"
        description =
            CVTextColor.MENU_DEFAULT_LORE + "Quickly switches all settings to the given preset"
        setControlListener(this@Rocktopus)
    }

    private val controls: List<OperatorControl> = listOf(
        buttonDispatch,
        buttonGates,
        buttonRideSpinDirection,
        buttonRideSpinSlower,
        buttonRideSpinFaster,
        buttonArmSpinDirection,
        buttonArmSpinSlower,
        buttonArmSpinFaster,
        buttonDiscSpinDirection,
        buttonDiscSpinSlower,
        buttonDiscSpinFaster,
        buttonArmLower,
        buttonArmHigher
    ) + presetButtons + reverseButton

    private val animator: ArmatureAnimator

    private var rideRotation = 0.0
    private var armRotation = 0.0

    private var rideRotationSpeed = 0.0
    private var armRotationSpeed = 0.0
    private var discRotationSpeed = 0.0
    private var armAngle = 0.0

    val targetArmAngle: Double
        get() = ARM_DEGREES[rideSettings.armDegrees]

    val targetRideRotationSpeed: Double
        get() = (if (rideSettings.rideForwards) 1 else -1) * RIDE_SPEEDS[rideSettings.rideSpeed]

    val targetArmRotationSpeed: Double
        get() = (if (rideSettings.armForwards) 1 else -1) * ARM_SPEEDS[rideSettings.armSpeed]

    val targetDiscRotationSpeed: Double
        get() = (if (rideSettings.discForwards) -1 else 1) * DISC_SPEEDS[rideSettings.discSpeed]

    init {
        for (entity in area.loc1.world!!.entities) {
            if (entity.name == rideName && area.isInArea(entity.location)) {
                entity.remove()
            }
        }

        val daeFile =
            File(CraftventureCore.getInstance().dataFolder, "data/ride/$NAME/base.dae")
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(daeFile)
        val armatures = DaeLoader.load(doc, "$NAME/ride")
        animator = armatures.map { ArmatureAnimator(it) }.first()
        val armature = animator.armature
        val root = armature.joints.first()
        val arm = root.childJoints.first()
        val disc = arm.childJoints.first().childJoints.first()
        val car = disc.childJoints.first()

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
        val carDegrees = 360.0 / ARM_COUNT
        for (i in 1 until CARS_PER_ARM)
            disc.childJoints.add(
                car.clone().apply { rotateAroundY(this.transform, matrix, quaternion, i * carDegrees) })

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

    private fun updateButtons() {
        updateRideSpeedButtons()
        updateArmSpeedButtons()
        updateDiscSpeedButtons()
        updateJumpHeightButtons()

        updateRideDirectionButton()
        updateArmDirectionButton()
        updateDiscDirectionButton()
    }

    private fun updateRideDirectionButton() {
        buttonRideSpinDirection.isOn = rideSettings.rideForwards
    }

    private fun updateArmDirectionButton() {
        buttonArmSpinDirection.isOn = rideSettings.armForwards
    }

    private fun updateDiscDirectionButton() {
        buttonDiscSpinDirection.isOn = rideSettings.discForwards
    }

    private fun updateRideSpeedButtons() {
        buttonRideSpinSlower.displayCount = max(rideSettings.rideSpeed + 1, 1)
        buttonRideSpinSlower.isEnabled = rideSettings.rideSpeed > 0
        buttonRideSpinFaster.displayCount = max(RIDE_SPEEDS.size - rideSettings.rideSpeed, 1)
        buttonRideSpinFaster.isEnabled = rideSettings.rideSpeed < RIDE_SPEEDS.lastIndex
    }

    private fun updateArmSpeedButtons() {
        buttonArmSpinSlower.displayCount = max(rideSettings.armSpeed + 1, 1)
        buttonArmSpinSlower.isEnabled = rideSettings.armSpeed > 0
        buttonArmSpinFaster.displayCount = max(ARM_SPEEDS.size - rideSettings.armSpeed, 1)
        buttonArmSpinFaster.isEnabled = rideSettings.armSpeed < ARM_SPEEDS.lastIndex
    }

    private fun updateDiscSpeedButtons() {
        buttonDiscSpinSlower.displayCount = max(rideSettings.discSpeed + 1, 1)
        buttonDiscSpinSlower.isEnabled = rideSettings.discSpeed > 0
        buttonDiscSpinFaster.displayCount = max(DISC_SPEEDS.size - rideSettings.discSpeed, 1)
        buttonDiscSpinFaster.isEnabled = rideSettings.discSpeed < DISC_SPEEDS.lastIndex
    }

    private fun updateJumpHeightButtons() {
        buttonArmLower.displayCount = max(rideSettings.armDegrees + 1, 1)
        buttonArmLower.isEnabled = rideSettings.armDegrees > 0
        buttonArmHigher.displayCount = max(ARM_DEGREES.size - rideSettings.armDegrees, 1)
        buttonArmHigher.isEnabled = rideSettings.armDegrees < ARM_DEGREES.lastIndex
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

    override fun prepareStart() {
        super.prepareStart()
        AudioServerApi.enable("rocktopus_onride")
        AudioServerApi.sync("rocktopus_onride", System.currentTimeMillis())
    }

    override fun start() {
        super.start()
        openGates(false)

        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()

        if (operator == null)
            rideSettings = rideSettings.copy(
                rideSpeed = max(rideSettings.rideSpeed, 1),
                armSpeed = max(rideSettings.armSpeed, 1),
                discSpeed = max(rideSettings.discSpeed, 1),
                armDegrees = max(rideSettings.armDegrees, 1),
                rideForwards = true,
                discForwards = true
            )

        exitGates.forEach { it.block.open(false) }

        rideSettings = presets.find { it.name == "Waltz" } ?: presets.randomOrNull()!!
    }

    override fun stop() {
        super.stop()
//        AudioServerApi.disable("swingride")
        if (!isBeingOperated)
            openGates(true)

        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()

        armAngle = 0.0
        armRotationSpeed = 0.0
        rideRotationSpeed = 0.0
        discRotationSpeed = 0.0

        exitGates.forEach { it.block.open(true) }

        AudioServerApi.disable("rocktopus_onride")
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
            } else if (operatorControl == buttonRideSpinDirection) {
                rideSettings = rideSettings.copy(rideForwards = !rideSettings.rideForwards)
            } else if (operatorControl == buttonRideSpinSlower) {
                rideSettings = rideSettings.copy(
                    rideSpeed = (rideSettings.rideSpeed - 1).clamp(0, RIDE_SPEEDS.lastIndex)
                )
            } else if (operatorControl == buttonRideSpinFaster) {
                rideSettings = rideSettings.copy(
                    rideSpeed = (rideSettings.rideSpeed + 1).clamp(0, RIDE_SPEEDS.lastIndex)
                )
            } else if (operatorControl == buttonArmSpinDirection) {
                rideSettings = rideSettings.copy(armForwards = !rideSettings.armForwards)
            } else if (operatorControl == buttonArmSpinSlower) {
                rideSettings = rideSettings.copy(
                    armSpeed = (rideSettings.armSpeed - 1).clamp(0, ARM_SPEEDS.lastIndex)
                )
            } else if (operatorControl == buttonArmSpinFaster) {
                rideSettings = rideSettings.copy(
                    armSpeed = (rideSettings.armSpeed + 1).clamp(0, ARM_SPEEDS.lastIndex)
                )
            } else if (operatorControl == buttonDiscSpinDirection) {
                rideSettings = rideSettings.copy(discForwards = !rideSettings.discForwards)
            } else if (operatorControl == buttonDiscSpinSlower) {
                rideSettings = rideSettings.copy(
                    discSpeed = (rideSettings.discSpeed - 1).clamp(0, DISC_SPEEDS.lastIndex)
                )
            } else if (operatorControl == buttonDiscSpinFaster) {
                rideSettings = rideSettings.copy(
                    discSpeed = (rideSettings.discSpeed + 1).clamp(0, DISC_SPEEDS.lastIndex)
                )
            } else if (operatorControl == buttonArmLower) {
                rideSettings = rideSettings.copy(
                    armDegrees = (rideSettings.armDegrees - 1).clamp(0, ARM_DEGREES.lastIndex)
                )
            } else if (operatorControl == buttonArmHigher) {
                rideSettings = rideSettings.copy(
                    armDegrees = (rideSettings.armDegrees + 1).clamp(0, ARM_DEGREES.lastIndex)
                )
            } else if (operatorControl == reverseButton) {
                rideSettings = rideSettings.reversed()
            }

            presetButtons.forEachIndexed { index, button ->
                if (button == operatorControl) {
                    rideSettings = presets[index]
                    return
                }
            }
        }
    }

    override fun updateCarts(forceTeleport: Boolean) {
//        Logger.debug(" \nUpdating carts ${rotation.format(2)}")
        cars.forEachAllocationless { it.updateTransforms() }
        animator.setTime(0.0)
        cars.forEachAllocationless { it.update() }
    }

//    private val shouldPickRandomPreset: Boolean
//        get() = operator == null && System.currentTimeMillis() > lastRandomPresetPick + PRESET_PICK_TIME

    override fun provideRunnable(): FlatrideRunnable = object : FlatrideRunnable() {
        override fun updateTick() {
//            if (shouldPickRandomPreset) {
//                pickRandomPreset()
//            }
            val programTime = System.currentTimeMillis() - startTime

            val stopping = programTime > RIDE_TIME_MILLIS

            val deltaRideRotationSpeed = ((if (stopping) 0.0 else targetRideRotationSpeed) - rideRotationSpeed)
                .clamp(
                    -RIDE_ROTATION_ACCELERATION,
                    RIDE_ROTATION_ACCELERATION
                )
            rideRotationSpeed += deltaRideRotationSpeed

            val deltaDiscRotationSpeed = ((if (stopping) 0.0 else targetDiscRotationSpeed) - discRotationSpeed)
                .clamp(
                    -DISC_ROTATION_ACCELERATION,
                    DISC_ROTATION_ACCELERATION
                )
            discRotationSpeed += deltaDiscRotationSpeed

            val deltaArmRotationSpeed = ((if (stopping) 0.0 else targetArmRotationSpeed) - armRotationSpeed)
                .clamp(
                    -ARM_ROTATION_ACCELERATION,
                    ARM_ROTATION_ACCELERATION
                )
            armRotationSpeed += deltaArmRotationSpeed

            rideRotation += rideRotationSpeed
            armRotation += armRotationSpeed

            val deltaArmAngle = ((if (stopping) 0.0 else targetArmAngle) - armAngle)
                .clamp(-ARM_ACCELERATION, ARM_ACCELERATION)
            armAngle += deltaArmAngle

            if (stopping && armAngle == 0.0 && armRotationSpeed == 0.0 && rideRotationSpeed == 0.0 && discRotationSpeed == 0.0) {
                stop()
                updateCarts(true)
//            Logger.debug(
//                "armAngle=${armAngle.format(2)}  armRotation=${armRotation.format(2)}"
//            )
            } else if (stopping && programTime > RIDE_TIME_MILLIS + RIDE_STOP_MILLIS) {
                stop()
                updateCarts(true)
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
        rotationFixer: RotationFixer,
        debug: Boolean = false
    ) {
        val position = jointTransform.transformPoint(calculators.vector.reset())
        entity.move(
            position.x,
            position.y - EntityConstants.ArmorStandHeadOffset,
            position.z
        )
//                            entity.customName = "$index ${headPose.x.format(2)}/${headPose.y.format(2)}/${headPose.z.format(2)}"

        rotationFixer.setNextRotation(jointTransform.rotation, debug)
        val rotationQuat = rotationFixer.getCurrentQuaternion()
        val rotation = TransformUtils.getArmorStandPose(rotationQuat)
//                            entity.headPose = EulerAngle(rotation.x, rotation.y, rotation.z)
        entity.head(rotation.x.toFloat(), rotation.y.toFloat(), rotation.z.toFloat())
    }

    class Arm(val joint: Joint, val armIndex: Int, val ride: Rocktopus) :
        FlatrideCar<ArmorStand>(arrayOfNulls(CARS_PER_ARM * SEATS_PER_CAR)) {
        val originalTransform = joint.transform.clone()
        val calculators = Calculators()
        val armMoveJoint = joint
        val armModelJoint = armMoveJoint.childJoints.first()
        val disc = Disc(armModelJoint.childJoints.first(), armIndex, ride = ride, arm = this)
        val relativeYaw = joint.transform.rotation.yawPitchRoll.y.let { if (it < 0) it + 360 else it }
        private val rotationFixer = RotationFixer()
        private var model: NpcEntity? = null
            set(value) {
                if (field != null)
                    ride.npcAreaTracker.removeEntity(field!!)
                field = value
                if (value != null)
                    ride.npcAreaTracker.addEntity(value)
            }

        fun updateTransforms() {
            joint.transform.set(originalTransform)
            ride.rotateAroundY(joint.transform, calculators.matrix, calculators.quaternion, ride.rideRotation)

            val currentRotation = (ride.rideRotation - relativeYaw + ride.armRotation) % 360.0
            val rotationPercentage = currentRotation / 360.0
            val percentage = ((sin(rotationPercentage * Math.PI * 2.0) + 1.0) / 2.0)
//            Logger.debug(
//                "arm=$armIndex currentRotation=${currentRotation.format(2)} rotationPercentage=${rotationPercentage.format(
//                    2
//                )} percentage=${percentage.format(2)}  relativeYaw=${relativeYaw.format(2)}"
//            )
            val upAngle = percentage.clamp(0.0, 1.0) * ride.armAngle
            joint.transform.rotateX(upAngle)

//            model?.customNameVisible(true)
//            model?.customName(
//                "arm=$armIndex currentRotation=${currentRotation.format(2)}"
//            )

            disc.updateTransforms(calculators)
        }

        fun update() {
            val entity = model ?: NpcEntity(
                "rocktopus",
                EntityType.ARMOR_STAND, Location(ride.area.world, CENTER.x, CENTER.y, CENTER.z)
            ).also {
                model = it
                it.invisible(true)
//                it.marker(true)
                it.helmet(MaterialConfig.ROCKTOPUS_ARM)
            }
            ride.applyRotationToEntity(armModelJoint.animatedTransform, calculators, entity, rotationFixer, true)
//            entity.customNameVisible(true)
//            entity.customName("$armIndex ${ride.rotation.format(2)}")
//            entity.location.spawnParticleX(Particle.FLASH)
//            Logger.debug("Arm=$armIndex ${ride.rotation.format(2)}")
            disc.update(calculators)
        }

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            update()
        }
    }

    class Disc(
        val joint: Joint,
        val index: Int,
        val ride: Rocktopus,
        val arm: Arm
    ) {
        val originalTransform = joint.transform.clone()
        val cars: Array<Car> = joint.childJoints
            .mapIndexed { carIndex, carJoint -> Car(carJoint, index, carIndex, ride = ride, disc = this) }
            .toTypedArray()
        var rotation = 0.0
        private val rotationFixer = RotationFixer()
        private var model: NpcEntity? = null
            set(value) {
                if (field != null)
                    ride.npcAreaTracker.removeEntity(field!!)
                field = value
                if (value != null)
                    ride.npcAreaTracker.addEntity(value)
            }

        fun updateTransforms(calculators: Calculators) {
            rotation += ride.discRotationSpeed
            joint.transform.set(originalTransform)
            joint.transform.rotateY(rotation)
//            ride.rotateAroundY(joint.animatedTransform, calculators.matrix, calculators.quaternion, rotation)
            cars.forEach { it.updateTransforms(calculators) }
        }

        fun update(calculators: Calculators) {
            val entity = model ?: NpcEntity(
                "rocktopus",
                EntityType.ARMOR_STAND, Location(ride.area.world, CENTER.x, CENTER.y, CENTER.z)
            ).also {
                model = it
                it.invisible(true)
                it.helmet(MaterialConfig.ROCKTOPUS_DISC)
            }
            ride.applyRotationToEntity(joint.animatedTransform, calculators, entity, rotationFixer)

            cars.forEach { it.update(calculators) }
        }
    }

    class Car(
        val joint: Joint,
        val armIndex: Int,
        val carIndex: Int,
        val ride: Rocktopus,
        val disc: Disc
    ) {
        val originalTransform = joint.transform.clone()
        val seats: Array<Seat> = joint.childJoints
            .mapIndexed { seatIndex, seatJoint -> Seat(seatJoint, armIndex, carIndex, seatIndex, ride, this) }
            .toTypedArray()

        var rotation = 0.0
        private val rotationFixer = RotationFixer()
        private var model: NpcEntity? = null
            set(value) {
                if (field != null)
                    ride.npcAreaTracker.removeEntity(field!!)
                field = value
                if (value != null)
                    ride.npcAreaTracker.addEntity(value)
            }

        fun updateTransforms(calculators: Calculators) {
            joint.transform.set(originalTransform)
            joint.transform.rotateY(rotation)
            seats.forEach { it.updateTransforms(calculators) }
        }

        fun update(calculators: Calculators) {
            val entity = model ?: NpcEntity(
                "rocktopus",
                EntityType.ARMOR_STAND, Location(ride.area.world, CENTER.x, CENTER.y, CENTER.z)
            ).also {
                model = it
                it.invisible(true)
                it.helmet(MaterialConfig.ROCKTOPUS_CAR)
            }
            ride.applyRotationToEntity(joint.animatedTransform, calculators, entity, rotationFixer)

            seats.forEach { it.update(calculators) }
        }
    }

    class Seat(
        val joint: Joint,
        val armIndex: Int,
        val carIndex: Int,
        val seatIndex: Int,
        val ride: Rocktopus,
        val car: Car
    ) {
        //        private val rotationFixer = RotationFixer()
        private val entityIndex = (carIndex * SEATS_PER_CAR) + seatIndex
        private val location = ride.exitLocation.clone()
        private val quaternion = Quaternion()

        fun updateTransforms(calculators: Calculators) {
        }

        fun update(calculators: Calculators) {
            val position = joint.animatedTransform.transformPoint(calculators.vector.reset())
//            Logger.debug(
//                "car $armIndex/$carIndex/$seatIndex ${position.x.format(2)} ${position.y.format(2)} ${position.z.format(
//                    2
//                )} >> ${joint.transform.toVector3().x.format(2)} ${joint.transform.toVector3().y.format(2)} ${joint.transform.toVector3().z.format(
//                    2
//                )} >> ${car.joint.animatedTransform.toVector3().x.format(2)} ${car.joint.animatedTransform.toVector3().y.format(
//                    2
//                )} ${car.joint.animatedTransform.toVector3().z.format(2)}"
//            )
            location.set(position.x, position.y, position.z, 0f, 0f)
            val entity = car.disc.arm.entities[entityIndex] ?: location.spawn<ArmorStand>().also {
                it.isPersistent = false
                it.isInvisible = true
                it.addDisabledSlots(*EquipmentSlot.values())
                car.disc.arm.entities[entityIndex] = it
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
        const val ARM_COUNT = 4
        const val CARS_PER_ARM = 4
        const val SEATS_PER_CAR = 2
        const val NAME = "rocktopus"
        val CENTER = Vector(-1185.5, 45.0, -825.5)
        const val BASE_RADIUS = 14.5
        const val RIDE_TIME_MILLIS = 60_000
        const val RIDE_STOP_MILLIS = 5_000

        val RIDE_SPEEDS = arrayOf(0.0, 0.5, 1.5, 2.5)
        const val RIDE_ROTATION_ACCELERATION = 0.05

        val DISC_SPEEDS = arrayOf(0.0, 1.0, 3.0, 5.0)
        const val DISC_ROTATION_ACCELERATION = 0.15

        val ARM_SPEEDS = arrayOf(0.0, 2.0, 4.0)
        const val ARM_ROTATION_ACCELERATION = 0.05

        val ARM_DEGREES = arrayOf(0.0, 10.0, 20.0, 35.0)
        const val ARM_ACCELERATION = (30.0 / 20.0 / 2.0)

        val PRESET_PICK_TIME = 15_000

        private var instance: Rocktopus? = null

        fun getInstance(): Rocktopus {
            if (instance == null) {
                instance = Rocktopus()
            }
            return instance!!
        }
    }

    data class RideSettingsState(
        val name: String = "default",
        val rideSpeed: Int = 0,
        val armSpeed: Int = 0,
        val discSpeed: Int = 0,
        val armDegrees: Int = 0,
        val rideForwards: Boolean = false,
        val armForwards: Boolean = false,
        val discForwards: Boolean = false
    ) {
        fun reversed() = copy(rideForwards = !rideForwards, armForwards = !rideForwards, discForwards = !discForwards)
        val isNonMoving = rideSpeed == 0 && armSpeed == 0 && discSpeed == 0 && armDegrees == 0
    }
}