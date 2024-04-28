package net.craftventure.core.ride.flatride

import net.craftventure.audioserver.api.AudioServerApi.disable
import net.craftventure.audioserver.api.AudioServerApi.enable
import net.craftventure.audioserver.api.AudioServerApi.sync
import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor.serverNotice
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.spawn
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.ride.flatride.MainstreetCaroussel.MainstreetCarousselSeat
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.OperatorAreaTracker
import net.craftventure.core.ride.operator.controls.ControlColors.NEUTRAL
import net.craftventure.core.ride.operator.controls.ControlColors.NEUTRAL_DARK
import net.craftventure.core.ride.operator.controls.OperatorButton
import net.craftventure.core.ride.operator.controls.OperatorControl
import net.craftventure.core.ride.operator.controls.OperatorLed
import net.craftventure.core.serverevent.PacketUseEntityEvent
import net.craftventure.core.utils.EntityUtils.teleport
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector


class MainstreetCaroussel private constructor() : Flatride<MainstreetCarousselSeat>(
    SimpleArea("world", ),
    Location(Bukkit.getWorld("world"), ),
    "carousel",
    "ride_mainstreet_carousel"
), OperableRide, OperatorControl.ControlListener {
    private val center: Vector = Vector()
    private var angle = 0.0
    private var speed = 0.0
    private val ACCELERATE_SPEED = Math.toRadians(1.0 / 20.0) // 1 degree per second
    private val MAX_SPEED = Math.toRadians(100.0 / 20.0) // 50 degree per second
    private val ledRunning // = new OperatorLed("running_indicator", this, Material.);
            : OperatorLed
    private val buttonDispatch: OperatorButton

    //    private OperatorSwitch buttonGates;
    //    private boolean gatesOpen = false;
    private var operator: Player? = null
    private val operatorArea = area!!
    override val operatorAreaTracker = OperatorAreaTracker(this, operatorArea)
    private fun updateRunningIndicator() {
        ledRunning.color = if (isRunning()) NEUTRAL else NEUTRAL_DARK
        ledRunning.isFlashing = isRunning()
    }

    private fun updateDispatchButton() {
        buttonDispatch.isEnabled = !isRunning() // && !gatesOpen);
    }

    //    private void updateGatesButton() {
    //        buttonGates.setEnabled(!isRunning());
    //        buttonGates.setOn(gatesOpen);
    //    }
    private fun openGates(open: Boolean) {
//        this.gatesOpen = open;
//        BlockUtils.open(doorEntrance1Block, open);
        updateDispatchButton()
    }

    private fun tryOperatorStart() {
        if (!isRunning()) { // && !gatesOpen) {
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
        //        updateGatesButton();
    }

    override fun stop() {
        super.stop()
        disable("mainstreet_carousel")
        if (!isBeingOperated) openGates(true)
        updateRunningIndicator()
        updateDispatchButton()
        //        updateGatesButton();
    }

    override fun prepareStart() {
        super.prepareStart()
        enable("mainstreet_carousel")
        sync("mainstreet_carousel", System.currentTimeMillis())
    }

    override fun updateCarts(forceTeleport: Boolean) {
        var index = 0
        for (seat in cars) {
            val isInnerRing = index < SEAT_COUNT_INNER_RING
            var seatAngle = angle
            seatAngle += if (isInnerRing) Math.toRadians(360.0 / SEAT_COUNT_INNER_RING.toDouble() * index) else Math.toRadians(
                360.0 / SEAT_COUNT_OUTER_RING.toDouble() * (index - SEAT_COUNT_INNER_RING)
            )
            val radius = if (isInnerRing) SEAT_OFFSET_INNER_RING.toDouble() else SEAT_OFFSET_OUTER_RING.toDouble()
            seat.teleport(
                center.x + radius * Math.cos(seatAngle),
                center.y + (Math.cos(3 * seatAngle + index) + 1) * 0.5,  // * (speed / MAX_SPEED)),
                center.z + radius * Math.sin(seatAngle),
                Math.toDegrees(seatAngle).toFloat(), 0f,
                forceTeleport
            )
            index++
        }
    }

    override fun teleportToExit(player: Player) {
//        super.teleportToExit(player);
    }

    override fun provideRunnable(): FlatrideRunnable {
        return object : FlatrideRunnable() {
            var frame = 0
            override fun updateTick() {
                frame++
                if (frame > 2) {
                    frame = 0
                    val deltaTime = System.currentTimeMillis() - startTime
                    if (deltaTime > 1000 * 30) {
                        stop()
                    } else {
                        if (deltaTime < 8000) {
                            speed += ACCELERATE_SPEED * 3
                            if (speed > MAX_SPEED) speed = MAX_SPEED
                        }
                        angle += speed
                        if (deltaTime > 22000) {
                            speed -= ACCELERATE_SPEED * 3
                            if (speed < 0) {
                                speed = 0.0
                                stop()
                            }
                        }
                        updateCarts(false)
                    }
                }
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
        //        controls.add(buttonGates);
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
            } // else if (operatorControl == buttonGates) {
            //                tryOpenGatesIfPossible(!buttonGates.isOn());
//            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPacketUseEntityEvent(event: PacketUseEntityEvent) {
        for (i in cars.indices) {
            val seat = cars[i]!!
            if (seat.armorStand != null && seat.armorStand!!.entityId == event.interactedEntityId /* ||
                    (seat.model != null && seat.model.getEntityId() == event.getInteractedEntityId())*/) {
                event.isCancelled = true
                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance()) {
                    if (seat.entities[0] != null) handleClick(
                        seat.entities[0], event.player
                    )
                }
            }
        }
    }

    inner class MainstreetCarousselSeat : FlatrideCar<ArmorStand?>(arrayOfNulls<ArmorStand>(1)) {
        val armorStand get() = entities[0]

        override fun teleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, forceTeleport: Boolean) {
            if (entities[0] == null || !entities[0]!!.isValid) {
                if (entities[0] != null) entities[0]!!.remove()
                entities[0] = Location(area.loc1.world, x, y - 0.2, z, yaw, pitch).spawn<ArmorStand>().apply {
                    equipment.helmet = dataItem(Material.DIAMOND_SWORD, 150)
                    isPersistent = false
                    setOwner(this@MainstreetCaroussel)
                    customName = rideName
                    setGravity(false)
                    addDisabledSlots(*EquipmentSlot.values())
                    isInvisible = true
                }
            }

            teleport(entities[0]!!, x, y - 0.6, z, yaw, pitch)
        }
    }

    companion object {
        private val SEAT_COUNT_INNER_RING get() = 4
        private val SEAT_OFFSET_INNER_RING get() = 4
        private val SEAT_COUNT_OUTER_RING get() = 8
        private val SEAT_OFFSET_OUTER_RING get() = 6
        private var _this: MainstreetCaroussel? = null

        @JvmStatic
        val instance: MainstreetCaroussel?
            get() {
                if (_this == null) _this = MainstreetCaroussel()
                return _this
            }
    }

    init {
        for (entity in area.loc1.world.entities) {
            if ((entity is ArmorStand || entity is Horse || entity is SkeletonHorse || entity is AbstractHorse) && area.isInArea(
                    entity.location
                )
            ) {
                entity.remove()
            }
        }
        cars.clear() // Just to be sure
        for (i in 0 until SEAT_COUNT_INNER_RING + SEAT_COUNT_OUTER_RING) {
            cars.add(MainstreetCarousselSeat())
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

//        buttonGates = new OperatorSwitch("gates");
//        buttonGates
//                .setName(CVChatColor.MENU_DEFAULT_TITLE + "Gates")
//                .setDescription(CVChatColor.MENU_DEFAULT_LORE + "Open the gates when the ride is not running")
//                .setControlListener(this);
        openGates(true)
    }
}