package net.craftventure.core.ride.flatride

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.AreaTracker
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.forEachAllocationless
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.protocol.ProtocolLeash
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorAreaTracker
import net.craftventure.core.ride.operator.controls.*
import net.craftventure.core.serverevent.PacketUseEntityEvent
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.EntityUtils.setInstantUpdate
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.entity.Rabbit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import penner.easing.Cubic
import penner.easing.Sine

class SwingRide private constructor() : Flatride<SwingRide.Chair>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    "swingride",
    "ride_swingride"
), OperableRide, OperatorControl.ControlListener {
    private val SEAT_COUNT = 14
    private val RADIUS = 7.0
    private val MAX_SPEED = Math.toRadians(60.0)
    private val HEIGHT_ROTATION_SPEED = -Math.toRadians(75.0)

    private val ROUND_TIME = 30000
    private val POWERUP_TIME = 7000
    private val SWING_POWERUP_TIME = 3000

    private val operatorArea = SimpleArea("world", )
    private val entranceGateLocation = Location(Bukkit.getWorld("world"), )
    private val centerLocation = Location(Bukkit.getWorld("world"), )

    private var operator: Player? = null
    private var gatesOpen = false
    protected var rideAngle: Double = 0.0
        private set

    protected var radiusOffset = 0.0
        private set
    protected var heightSwing = 0.0
        private set
    protected var heightOffset = 0.0
    protected var heightRotationAngle = 0.0
    override val operatorAreaTracker: AreaTracker = OperatorAreaTracker(this, operatorArea)
    private val ledRunning = OperatorLed("running_indicator", ControlColors.NEUTRAL_DARK).apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Running"
        description = CVTextColor.MENU_DEFAULT_LORE + "Indicates wether the ride is running or not"
        setControlListener(this@SwingRide)
    }
    private val buttonDispatch = OperatorButton("dispatcher", OperatorButton.Type.DEFAULT).apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Start"
        isFlashing = true
        description =
            CVTextColor.MENU_DEFAULT_LORE + "Starts the ride if it's not started yet. Requires gates to be closed"
        setControlListener(this@SwingRide)
    }
    private val buttonGates = OperatorSwitch("gates").apply {
        name = CVTextColor.MENU_DEFAULT_TITLE + "Gates"
        description = CVTextColor.MENU_DEFAULT_LORE + "Open the gates when the ride is not running"
        setControlListener(this@SwingRide)
    }

    init {
        for (entity in area.loc1.world!!.entities) {
            if (entity.name == rideName && area.isInArea(entity.location)) {
                entity.remove()
            }
        }

        cars.clear()
        for (i in 0 until SEAT_COUNT) {
            cars.add(Chair(Math.toRadians(360.0 / SEAT_COUNT.toFloat() * i)))
        }

        updateCarts(true)

        openGates(true)
    }

    private fun updateRunningIndicator() {
        ledRunning.color = if (isRunning()) ControlColors.NEUTRAL else ControlColors.NEUTRAL_DARK
        ledRunning.isFlashing = isRunning()
    }

    private fun updateDispatchButton() {
        buttonDispatch.isEnabled = !isRunning() && !gatesOpen
    }

    private fun updateGatesButton() {
        buttonGates.isEnabled = !isRunning()
        buttonGates.isOn = gatesOpen
    }

    private fun openGates(open: Boolean) {
        this.gatesOpen = open
        entranceGateLocation.block.open(open)
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
    }

    override fun stop() {
        super.stop()
//        AudioServerApi.disable("swingride")
        if (!isBeingOperated)
            openGates(true)

        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()
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

    override fun provideControls(): MutableList<OperatorControl> =
        mutableListOf(ledRunning, buttonGates, buttonDispatch)

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
            }
        }
    }

    override fun updateCarts(forceTeleport: Boolean) {
        val position = Vector()
        cars.forEachAllocationless { car ->
            val heightSwingHeightOffset = (Math.sin(car.angle + rideAngle + heightRotationAngle) * heightSwing * 2)
            position.x = centerLocation.x + (Math.cos(car.angle + rideAngle) * (RADIUS + radiusOffset))
            position.y = 38.0 + heightOffset + heightSwingHeightOffset
            position.z = centerLocation.z + (Math.sin(car.angle + rideAngle) * (RADIUS + radiusOffset))

            car.teleport(position.x, position.y, position.z, heightSwingHeightOffset.toFloat(), 0f, forceTeleport)
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPacketUseEntityEvent(event: PacketUseEntityEvent) {
//        Logger.info("Received entity use packet event for ${event.interactedEntityId}")
        for (i in cars.indices) {
            val seat = cars[i]
            if (seat.leftHitch?.entityId == event.interactedEntityId || seat.rightHitch?.entityId == event.interactedEntityId) {
                event.isCancelled = true
                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance()) {
                    if (seat.entities[0] != null)
                        handleClick(seat.entities[0], event.player)
                }
                return
            }
        }
    }

    override fun provideRunnable(): FlatrideRunnable = object : FlatrideRunnable() {
        //        var frame = 0
        var speed = 0.0

        override fun updateTick() {
//            frame++
//            if (frame > 2) {
//                frame = 0
//                frame++
            val programTime = System.currentTimeMillis() - startTime

            val powerupT = when {
                programTime < POWERUP_TIME -> {
                    Cubic.easeIn(programTime.toDouble(), 0.0, 1.0, POWERUP_TIME.toDouble()).clamp(0.0, 1.0)
                }
                programTime > POWERUP_TIME + SWING_POWERUP_TIME + ROUND_TIME + SWING_POWERUP_TIME -> {
                    1 - Sine.easeOut(
                        programTime.toDouble() - (POWERUP_TIME + SWING_POWERUP_TIME + ROUND_TIME + SWING_POWERUP_TIME),
                        0.0,
                        1.0,
                        POWERUP_TIME.toDouble()
                    ).clamp(0.0, 1.0)
                }
                else -> 1.0
            }.clamp(0.0, 1.0)

            val swingT = when {
                programTime > POWERUP_TIME && programTime < POWERUP_TIME + SWING_POWERUP_TIME -> {
                    (programTime - POWERUP_TIME).toDouble() / SWING_POWERUP_TIME.toDouble()
                }
                programTime > POWERUP_TIME + SWING_POWERUP_TIME + ROUND_TIME && programTime < POWERUP_TIME + SWING_POWERUP_TIME + ROUND_TIME + SWING_POWERUP_TIME -> {
                    1 - ((programTime - (POWERUP_TIME + SWING_POWERUP_TIME + ROUND_TIME)).toDouble() / SWING_POWERUP_TIME.toDouble())
                }
                programTime >= POWERUP_TIME + SWING_POWERUP_TIME && programTime < POWERUP_TIME + SWING_POWERUP_TIME + ROUND_TIME + SWING_POWERUP_TIME -> {
                    1.0
                }
                else -> {
                    0.0
                }
            }.clamp(0.0, 1.0)

            heightOffset = powerupT * 6.0
            radiusOffset = powerupT * 2.0
            speed = powerupT * MAX_SPEED

            heightSwing = swingT

            if (programTime > POWERUP_TIME + SWING_POWERUP_TIME + ROUND_TIME + SWING_POWERUP_TIME + POWERUP_TIME) {
                speed = 0.0
                heightOffset = 0.0
                stop()
                updateCarts(true)
            }

            heightRotationAngle -= (HEIGHT_ROTATION_SPEED * 0.05)
            rideAngle += (speed * 0.05)
            updateCarts(false)
//            }
        }
    }

    inner class Chair(
        val angle: Double
    ) : FlatrideCar<ArmorStand>(arrayOfNulls(1)) {
        val loc = centerLocation.clone()
        var topHitch: ArmorStand? = null
        var leftHitch: Rabbit? = null
        var rightHitch: Rabbit? = null
        var leftLeash: ProtocolLeash? = null
        var rightLeash: ProtocolLeash? = null

        init {
            loc.pitch = 0f
        }

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            loc.x = x
            loc.y = y
            loc.z = z
            loc.yaw = Math.toDegrees(angle + rideAngle + (-yaw * 0.3f).clamp(-1.0f, 0.2f)).toFloat()

            if (entities[0] == null || !entities[0].isValid) {
                entities[0] = loc.spawn()
                entities[0].setOwner(this@SwingRide)
                entities[0].setGravity(false)
                entities[0].setBasePlate(false)
                entities[0].isVisible = false
                entities[0].customName = this@SwingRide.rideName
                entities[0].setHelmet(MaterialConfig.SWINGRIDE_CHAIR)
                entities[0].addDisabledSlots(*EquipmentSlot.values())
            } else {
                EntityUtils.teleport(entities[0], loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
                entities[0].headPose = EulerAngle(0.0, 0.0, (-radiusOffset * 0.15).clamp(-0.5, 0.1))
            }


            var recreateHitches = false
            loc.x = centerLocation.x + (Math.cos(angle + rideAngle) * (RADIUS + 0.5))
            loc.y = 52.0
            loc.z = centerLocation.z + (Math.sin(angle + rideAngle) * (RADIUS + 0.5))
            if (topHitch == null || topHitch?.isValid == false) {
                topHitch = loc.spawn()
                topHitch!!.setOwner(this@SwingRide)
                topHitch!!.setGravity(false)
                topHitch!!.setAI(false)
                topHitch!!.isSilent = true
                topHitch!!.isInvulnerable = true
                topHitch!!.isVisible = false
                topHitch!!.setBasePlate(false)
                topHitch!!.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.INVISIBILITY,
                        Integer.MAX_VALUE,
                        1,
                        true,
                        false
                    )
                )
                topHitch!!.customName = this@SwingRide.rideName
                topHitch!!.addDisabledSlots(*EquipmentSlot.values())

                recreateHitches = true
            } else {
                EntityUtils.teleport(
                    topHitch!!,
                    loc.x,
                    loc.y,
                    loc.z,
                    loc.yaw,
                    loc.pitch
                )
            }


            loc.x = centerLocation.x + (Math.cos(angle + rideAngle) * (RADIUS + radiusOffset + 0.4))
            loc.y = y + 1.7
            loc.z = centerLocation.z + (Math.sin(angle + rideAngle) * (RADIUS + radiusOffset + 0.4))
            if (leftHitch == null || leftHitch?.isValid == false) {
                leftHitch = loc.spawn()
                leftHitch!!.setInstantUpdate()
                leftHitch!!.setOwner(this@SwingRide)
                leftHitch!!.setGravity(false)
                leftHitch!!.setAI(false)
                leftHitch!!.isSilent = true
                leftHitch!!.isInvulnerable = true
                leftHitch!!.isCollidable = false
                leftHitch!!.isInvisible = true
                leftHitch!!.customName = this@SwingRide.rideName

                recreateHitches = true
            } else {
                EntityUtils.teleport(
                    leftHitch!!,
                    loc.x,
                    loc.y,
                    loc.z,
                    loc.yaw,
                    loc.pitch
                )
            }

            loc.x = centerLocation.x + (Math.cos(angle + rideAngle) * (RADIUS + radiusOffset - 0.5))
            loc.y = y + 1.7
            loc.z = centerLocation.z + (Math.sin(angle + rideAngle) * (RADIUS + radiusOffset - 0.5))
            if (rightHitch == null || rightHitch?.isValid == false) {
                rightHitch = loc.spawn()
                rightHitch!!.setInstantUpdate()
                rightHitch!!.setOwner(this@SwingRide)
                rightHitch!!.setGravity(false)
                rightHitch!!.setAI(false)
                rightHitch!!.isSilent = true
                rightHitch!!.isInvulnerable = true
                rightHitch!!.isCollidable = false
                rightHitch!!.isInvisible = true
                rightHitch!!.customName = this@SwingRide.rideName

                recreateHitches = true
            } else {
                EntityUtils.teleport(
                    rightHitch!!,
                    loc.x,
                    loc.y,
                    loc.z,
                    loc.yaw,
                    loc.pitch
                )
            }

            if (recreateHitches) {
                leftLeash?.destroy()
                leftLeash = ProtocolLeash(leftHitch!!.entityId, topHitch!!.entityId)
                leftLeash?.create()

                rightLeash?.destroy()
                rightLeash = ProtocolLeash(rightHitch!!.entityId, topHitch!!.entityId)
                rightLeash?.create()
            }
        }
    }

    companion object {
        private var _instance: SwingRide? = null

        fun getInstance(): SwingRide {
            if (_instance == null) {
                _instance = SwingRide()
            }
            return _instance!!
        }
    }
}