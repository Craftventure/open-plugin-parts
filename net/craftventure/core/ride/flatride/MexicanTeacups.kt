package net.craftventure.core.ride.flatride

import net.craftventure.audioserver.api.AudioServerApi.disable
import net.craftventure.audioserver.api.AudioServerApi.enable
import net.craftventure.audioserver.api.AudioServerApi.sync
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor.serverNotice
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.ride.flatride.MexicanTeacups.MexicanTeacupSeat
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorAreaTracker
import net.craftventure.core.ride.operator.controls.ControlColors.NEUTRAL
import net.craftventure.core.ride.operator.controls.ControlColors.NEUTRAL_DARK
import net.craftventure.core.ride.operator.controls.OperatorButton
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.operator.controls.OperatorControl.ControlListener
import net.craftventure.core.ride.operator.controls.OperatorLed
import net.craftventure.core.ride.operator.controls.OperatorSwitch
import net.craftventure.core.serverevent.PacketPlayerSteerEvent
import net.craftventure.core.utils.EntityUtils.teleport
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.util.*


class MexicanTeacups private constructor() : Flatride<MexicanTeacupSeat>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    "tacotime",
    "ride_tacotime"
), OperableRide, ControlListener {
    private val center = Vector()
    private val disks: MutableList<MexicanTeacupDisk> = LinkedList()
    private var angle = 0.0
    private var speed = 0.0
    private val ACCELERATE_SPEED = Math.toRadians(1.0 / 20.0) // 1 degree per second
    private val MAX_SPEED = Math.toRadians(50.0 / 20.0) // 50 degree per second
    private val DISK_ANGLE_PART = Math.toRadians(360.0 / DISK_COUNT.toDouble())
    private val SEAT_ANGLE_PART = Math.toRadians(360.0 / SEAT_COUNT_PER_DISK.toDouble())
    private val entranceGate = Bukkit.getWorld("world")!!.getBlockAt(222, 42, -599)
    private val ledRunning // = new OperatorLed("running_indicator", this, Material.);
            : OperatorLed
    private val buttonDispatch: OperatorButton
    private val buttonGates: OperatorSwitch
    private var gatesOpen = false
    private var operator: Player? = null
    private val operatorArea = area
    override val operatorAreaTracker = OperatorAreaTracker(this, operatorArea)
    private fun updateRunningIndicator() {
        ledRunning.color = if (isRunning()) NEUTRAL else NEUTRAL_DARK
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
        gatesOpen = open
        entranceGate.open(open)
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

    override fun shouldCancel(clickedId: Entity, player: Player): Boolean {
        for (disk in disks) {
            if (disk.model!!.entityId == clickedId.entityId) {
                return true
            }
        }
        return super.shouldCancel(clickedId, player)
    }

    private val cupModel
        get() = MaterialConfig.dataItem(Material.DIAMOND_SWORD, 69)
    private val diskModel
        get() = MaterialConfig.dataItem(Material.DIAMOND_SWORD, 70)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSteer(event: PacketPlayerSteerEvent) {
//        super.onEntityDismountEvent(event);
        if (event.sideways != 0f) {
            for (teacupSeat in cars) {
                if (teacupSeat!!.isPassenger(event.player)) {
                    teacupSeat.speed += if (event.sideways > 0) 0.05 else -0.05
                    teacupSeat.speed = Math.min(2.0, Math.max(-2.0, teacupSeat.speed))
                    //                    Logger.console("New speed is " + teacupSeat.speed);
                }
            }
        }
    }

    override fun provideRunnable(): FlatrideRunnable {
        return object : FlatrideRunnable() {
            var frame = 0
            override fun updateTick() {
                frame++
                if (frame > 2) {
                    frame = 0
                    val deltaTime = System.currentTimeMillis() - startTime
                    if (deltaTime > 50000) {
                        stop()
                    } else {
                        if (deltaTime < 8000) {
                            speed += ACCELERATE_SPEED * 3
                            if (speed > MAX_SPEED * 3) speed = MAX_SPEED * 3
                        }
                        angle += speed
                        if (deltaTime > 42000) {
                            speed -= ACCELERATE_SPEED * 3
                            if (speed < 0) {
                                speed = 0.0
                                updateCarts(true)
                                stop()
                                return
                            }
                        }
                        updateCarts(false)
                    }
                }
            }
        }
    }

    override fun prepareStart() {
        super.prepareStart()
        for (seat in cars) {
            seat!!.prepareStart()
        }
        val centerLocation = Location(Bukkit.getWorld("world"), center.x, center.y, center.z)
        for (player in Bukkit.getOnlinePlayers()) {
            if (player !== operator && player.vehicle == null && player.location.y > 40 && player.location.y < 45 && centerLocation.distanceSquared(
                    player.location
                ) <= 13 * 13
            ) {
                player.teleport(exitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                //                Logger.console("Teleport " + player.getName());
            }
        }
        enable("tacotime")
        sync("tacotime", System.currentTimeMillis())
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
        disable("tacotime")
        if (!isBeingOperated) openGates(true)
        updateRunningIndicator()
        updateDispatchButton()
        updateGatesButton()
    }

    override fun updateCarts(forceTeleport: Boolean) {
        var seatIndex = 0
        for (d in 0 until DISK_COUNT) {
            val disk = disks[d]
            val diskAngle = angle + DISK_ANGLE_PART * d
            val diskX = center.x + DISK_OFFSET * Math.cos(diskAngle)
            val diskY = center.y
            val diskZ = center.z + DISK_OFFSET * Math.sin(diskAngle)

//            Logger.console("Speed " + speed);
            if (speed > 0.09) {
                disk.angle -= (speed - 0.09) * 4
            } else {
                disk.angle += speed
            }
            disk.teleport(diskX, diskY - 1.08, diskZ, forceTeleport)
            for (s in 0 until SEAT_COUNT_PER_DISK) {
                val seatAngle = disk.angle + SEAT_ANGLE_PART * s
                val seat = cars[seatIndex]!!
                if (speed > 0.06) {
                    seat.cupAngle -= (seat.speed * (speed - 0.06) * 2).toFloat()
                }
                seat.teleport(
                    diskX + SEAT_OFFSET * Math.cos(seatAngle),
                    diskY - 0.25,
                    diskZ + SEAT_OFFSET * Math.sin(seatAngle),
                    Math.toDegrees(seatAngle).toFloat(), 0f,
                    forceTeleport
                )
                seatIndex++
            }
        }
    }

    private inner class MexicanTeacupDisk {
        var angle = Random().nextDouble()
        var model: ArmorStand? = null
        fun teleport(x: Double, y: Double, z: Double, forceTeleport: Boolean) {
//            Logger.console(" ");
            if (model == null || !model!!.isValid) {
                model = area.loc1.world.spawn(
                    Location(
                        area.loc1.world,
                        x,
                        y,
                        z,
                        Math.toDegrees(angle).toFloat(),
                        0f
                    ), ArmorStand::class.java
                )
                model!!.isPersistent = false
                model!!.setOwner(this@MexicanTeacups)
                model!!.customName = rideName
                model!!.setGravity(false)
                model!!.setHelmet(diskModel)
                model!!.setBasePlate(false)
                model!!.isVisible = false
                model!!.addDisabledSlots(*EquipmentSlot.values())
            } else { //if (forceTeleport) {
                teleport(model!!, x, y, z, Math.toDegrees(angle).toFloat(), 0f)
            }
            if (forceModelUpdate) {
                model!!.setHelmet(diskModel)
            }
        }
    }

    override val isBeingOperated: Boolean
        get() = operator != null

    override fun getOperatorForSlot(slot: Int): Player? {
        return if (slot == 0) operator else null
    }

    override fun getOperatorSlot(player: Player): Int {
        return if (player === operator) 0 else -1
    }

    override fun setOperator(player: Player, slot: Int): Boolean {
        if (slot == 0 && operator == null) {
            operator = player
            if (operator != null) operator!!.sendMessage(serverNotice + "You are now operating " + ride!!.displayName)
            cancelAutoStart()
            return true
        }
        return false
    }

    override val totalOperatorSpots: Int
        get() = 1

    override fun cancelOperating(slot: Int) {
        if (slot == 0) {
            if (operator != null) {
                operator!!.sendMessage(serverNotice + "You are no longer operating " + ride!!.displayName)
                operator = null
                scheduleAutoStart()
                tryOpenGatesIfPossible(true)
            }
        }
    }

    override fun provideControls(): List<OperatorControl> {
        val controls: MutableList<OperatorControl> = ArrayList()
        controls.add(ledRunning)
        controls.add(buttonGates)
        controls.add(buttonDispatch)
        return controls
    }

    override fun isInOperateableArea(location: Location): Boolean {
        return operatorArea.isInArea(location)
    }

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
            } else if (operatorControl === buttonGates) {
                tryOpenGatesIfPossible(!buttonGates.isOn)
            }
        }
    }

    inner class MexicanTeacupSeat(loc: Location) : FlatrideCar<ArmorStand?>(
        arrayOfNulls<ArmorStand>(
            SEAT_COUNT_PER_CUP
        )
    ) {
        var cupAngle = (Random().nextDouble() * Math.PI * 2).toFloat()
        private val angleParCar = Math.toRadians(360.0 / SEAT_COUNT_PER_CUP)
        var speed = 0.0
        fun prepareStart() {
            val multiplier = 2.0
            speed = 0.05 + (multiplier * 2 * Random().nextDouble() - multiplier)
            for (armorStand in entities) {
                if (armorStand != null && armorStand.passenger is Player) {
                    val player = armorStand.passenger as Player?
                    player?.sendTitleWithTicks(
                        20,
                        20 * 5,
                        20,
                        NamedTextColor.GOLD,
                        "",
                        NamedTextColor.YELLOW,
                        "Hold A or D to spin your cup"
                    )
                    //                    MessageBarManager.display(player,
//                            ChatUtils.INSTANCE.createComponent("Hold A/D to control your speed of your cup", CVChatColor.RIDE_NOTICE),
//                            MessageBarManager.Type.RIDE,
//                            TimeUtils.secondsFromNow(10),
//                            ChatUtils.INSTANCE.getID_RIDE());
                }
            }
        }

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            val yawRadians = Math.toRadians(yaw.toDouble()).toFloat()
            //            Logger.console(" ");
            for (i in 0 until SEAT_COUNT_PER_CUP) {
//                Logger.console("Yaw " + (float) (yawRadian + Math.toDegrees(cupAngle + (angleParCar * i))));
                if (entities[i] == null || !entities[i]!!.isValid) {
                    entities[i] = area.loc1.world.spawn(
                        Location(
                            area.loc1.world,
                            x + SEAT_RADIUS * Math.cos(cupAngle + yawRadians - angleParCar * i),
                            y,
                            z + SEAT_RADIUS * Math.sin(cupAngle + yawRadians - angleParCar * i),
                            (90 + Math.toDegrees(cupAngle + yawRadians - angleParCar * i)).toFloat(),
                            pitch
                        ), ArmorStand::class.java
                    )
                    entities[i]!!.isPersistent = false
                    entities[i]!!.customName = rideName
                    entities[i]!!.setOwner(this@MexicanTeacups)
                    entities[i]!!.setGravity(false)
                    if (i == 0) entities[i]!!.setHelmet(cupModel)
                    entities[i]!!.setBasePlate(false)
                    entities[i]!!.isVisible = false
                    entities[i]!!.addDisabledSlots(*EquipmentSlot.values())
                } else if (entities[i]!!.helmet != null || entities[i]!!.passenger != null || forceTeleport) {
                    teleport(
                        entities[i]!!,
                        x + SEAT_RADIUS * Math.cos(cupAngle + yawRadians - angleParCar * i),
                        y,
                        z + SEAT_RADIUS * Math.sin(cupAngle + yawRadians - angleParCar * i),
                        (90 + Math.toDegrees(cupAngle + yawRadians - angleParCar * i)).toFloat(),
                        pitch
                    )
                }
                if (forceModelUpdate) {
                    if (i == 0) entities[i]!!.setHelmet(cupModel)
                }
            }
        }

        init {
            var loc = loc
            loc = loc.clone()
            loc.x = loc.x + 2
            loc.y = loc.y + 0.2
            loc.z = loc.z + 2
            for (i in 0 until SEAT_COUNT_PER_CUP) {
//                seats[i].setHelmet(new ItemStack(Material.ACACIA_STAIRS, 1, (byte) 4));
            }
            prepareStart()
        }
    }

    companion object {
        private const val DISK_COUNT = 4
        private const val SEAT_COUNT_PER_DISK = 3
        private const val SEAT_COUNT_PER_CUP = 3
        private val SEAT_RADIUS
            get() = 0.75
        private const val DISK_OFFSET = 5.5
        private const val SEAT_OFFSET = 2.0
        private var _this: MexicanTeacups? = null

        @JvmStatic
        val instance: MexicanTeacups?
            get() {
                if (_this == null) _this = MexicanTeacups()
                return _this
            }
    }

    init {
        for (entity in area.loc1.world.entities) {
            if (entity is ArmorStand && area.isInArea(entity.getLocation())) {
                entity.remove()
            }
        }
        cars.clear() // Just to be sure
        for (i in 0 until DISK_COUNT * SEAT_COUNT_PER_DISK) {
            cars.add(MexicanTeacupSeat(area.loc1))
        }
        for (i in 0 until DISK_COUNT) {
            disks.add(MexicanTeacupDisk())
        }
        updateCarts(true)
        ledRunning = OperatorLed("running_indicator", NEUTRAL_DARK)
        ledRunning
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Running")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "Indicates wether the ride is running or not")
            .setControlListener(this)
        buttonDispatch = OperatorButton("dispatcher", OperatorButton.Type.DEFAULT)
        buttonDispatch
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Start")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "Starts the ride if it's not started yet. Requires gates to be closed")
            .setControlListener(this)
        buttonGates = OperatorSwitch("gates")
        buttonGates
            .setName(CVTextColor.MENU_DEFAULT_TITLE + "Gates")
            .setDescription(CVTextColor.MENU_DEFAULT_LORE + "Open the gates when the ride is not running")
            .setControlListener(this)
        openGates(true)
    }
}