package net.craftventure.core.ride.operator

import net.craftventure.bukkit.ktx.extension.rotateAroundY
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.spawn
import net.craftventure.core.ride.operator.controls.OperatorControl
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Lectern
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Rabbit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.EulerAngle

class LecternPanel(
    location: Location,
    val rows: Int = 3,
    val columns: Int = 5,
    val rotation: Rotation,
    val ride: OperableRide,
    val controlFilter: ControlFilter = DefaultControlFilter()
) : Listener, OperatorManager.ControlInvalidatedListener, OperatorManager.OperatorInvalidatedListener {
    private val block = location.block
    private val location = block.location
    private val blockFloorCenter = this.location.clone().add(0.5, 0.0, 0.5).toVector()
    private var created: Boolean = false

    private val displayDepth = 12.0 / 16.0
    private val startHeight = 13.5 / 16.0
    private val buttonSizeWidth = 1.0 / columns.toDouble()
    private val buttonSizeDepth = displayDepth / rows.toDouble()
    private val heightDifference = 5.0 / 16.0

    private val buttons = run {
        (0 until columns).flatMap { x ->
            (0 until rows).map { y ->
                Button(
                    this.location
                        .clone()
                        .add(
                            1 - (buttonSizeWidth * 0.5 + buttonSizeWidth * x),
                            startHeight + (((rows - y - 0.35) / rows.toDouble()) * heightDifference),
                            1 - (1 - displayDepth) - (buttonSizeDepth * 0.35 + buttonSizeDepth * y)
                        )
                        .rotateAroundY(blockFloorCenter, Math.toRadians(-rotation.rotation))
                        .apply {
                            yaw = (rotation.rotation + 180.0).toFloat() % 360f
                        }
                )
            }
        }.toTypedArray()
    }

    private fun buttonIndex(x: Int, y: Int) = (x * rows) + y

    private fun indexOfButton(button: Button) = buttons.indexOf(button)

    override fun onControlUpdated(ride: OperableRide, operatorControl: OperatorControl) {
        if (ride !== this.ride) return
        updateControl(operatorControl)
    }

    override fun onOperatorUpdated(ride: OperableRide, slot: Int, player: Player?) {
        if (ride !== this.ride) return
        updateOperators()
    }

    private fun updateButton(button: Button) {
        val control = button.operatorControl ?: return
        button.model?.setHelmet(control.representAsItemStack())
    }

    private fun updateControl(operatorControl: OperatorControl) {
        val button = buttons.firstOrNull { it.operatorControl == operatorControl }
        if (button != null) {
            updateButton(button)
        }
    }

    private fun updateControls() {
        buttons.forEach {
            updateButton(it)
        }
    }

    private fun updateOperators() {
        // TODO
    }

    private fun reassignControls(controls: List<OperatorControl>) {
        var row = 0
        var column = ride.totalOperatorSpots
        if (column >= columns) {
            row++
            column = 0
        }

        var group: String? = controls.firstOrNull()?.group
        for (operatorControl in controls) {
            if (group != operatorControl.group) {
                row++
                column = 0
            }

            val button = buttons[buttonIndex(column, row)]
            button.operatorControl = operatorControl

            column++

            if (column >= columns) {
                row++
                column = 0
            }

            group = operatorControl.group
            if (row >= rows) break
        }

        updateControls()
    }

    private fun ensureEntities(x: Int, y: Int) {
        val index = buttonIndex(x, y)
//        Logger.debug("$index for $x/$y")
        val button = buttons[index]

        val buttonLocation = button.location

        if (button.clickHandler == null || !button.clickHandler!!.isValid) {
            button.clickHandler = buttonLocation
                .clone()
                .add(0.0, -0.2, 0.0)
                .spawn<Rabbit>()
                .apply {
                    //                    customName = "Dispatch"
                    setGravity(false)
                    setAI(false)
                    isInvulnerable = true
                    rabbitType = Rabbit.Type.WHITE
                    noDamageTicks = Int.MAX_VALUE
                    setBaby()
                    isInvisible = true
                }
        }

        if (button.model == null || !button.model!!.isValid) {
            button.model = buttonLocation
                .clone()
                .add(0.0, -1.45, 0.0)
                .spawn<ArmorStand>()
                .apply {
                    addDisabledSlots(*EquipmentSlot.values())
                    setGravity(false)
                    setAI(false)
                    fireTicks = Int.MAX_VALUE
                    isMarker = true
//                    customName = "$x, $y"
//                    isCustomNameVisible = true
                    isInvisible = true
                    isSilent = true
                    isInvulnerable = true
                    headPose = EulerAngle(Math.toRadians(22.5), 0.0, 0.0)

                }
            updateButton(button)
        }
    }

    private fun handleClick(player: Player, button: Button) {
//        val index = indexOfButton(button)
//        Logger.debug("Button $index pressed by ${player.name}")
        val result = button.operatorControl?.click(ride, player)
//        if (result == false) {
//            button.location.spawnParticleX(
//                Particle.REDSTONE,
//                data = Particle.DustOptions(Color.RED, 1.0f)
//            )
//        }/* else {
//            button.location.spawnParticleX(
//                Particle.REDSTONE,
//                data = Particle.DustOptions(Color.GREEN, 1.0f)
//            )
//        }*/
    }

    @EventHandler
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val entity = event.entity

        buttons.forEach { button ->
            if (button.clickHandler?.entityId == entity.entityId) {
                event.isCancelled = true
                handleClick(player, button)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        val entity = event.rightClicked

        buttons.forEach { button ->
            if (button.clickHandler?.entityId == entity.entityId) {
                event.isCancelled = true
                handleClick(player, button)
            }
        }
    }

    fun create() {
        if (created) return
        block.blockData = (Material.LECTERN.createBlockData() as Lectern)
            .apply {
                facing = rotation.blockFace
            }

        for (x in 0 until columns) {
            for (y in 0 until rows) {
                ensureEntities(x, y)
//                val button = buttons[buttonIndex(x, y)]
            }
        }
        reassignControls(ride.provideControls()
            .sortedBy { it.sort }
            .sortedBy { it.group })
        Bukkit.getServer().pluginManager.registerEvents(this, CraftventureCore.getInstance())
        CraftventureCore.getOperatorManager().controlInvalidatedListener += this
        created = true
    }

    fun destroy() {
        if (!created) return
        HandlerList.unregisterAll(this)
        CraftventureCore.getOperatorManager().controlInvalidatedListener -= this
        created = false
    }

    enum class Rotation(val blockFace: BlockFace, val rotation: Double) {
        NORTH(BlockFace.NORTH, 0.0),
        EAST(BlockFace.EAST, 90.0),
        SOUTH(BlockFace.SOUTH, 180.0),
        WEST(BlockFace.WEST, 270.0)
    }

    class Button(val location: Location) {
        var operatorControl: OperatorControl? = null
        var model: ArmorStand? = null
        var clickHandler: Entity? = null
    }

    interface ControlFilter {
        fun provideFilteredControls(ride: OperableRide): List<OperatorControl>
    }

    class DefaultControlFilter : ControlFilter {
        override fun provideFilteredControls(ride: OperableRide): List<OperatorControl> = ride.provideControls()
    }
}