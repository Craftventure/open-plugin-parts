package net.craftventure.core.feature.kart

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.extension.reset
import net.craftventure.bukkit.ktx.extension.set
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.animation.armature.Joint
import net.craftventure.core.config.AreaConfigManager
import net.craftventure.core.extension.getFirstPassenger
import net.craftventure.core.extension.hasPassengers
import net.craftventure.core.feature.kart.addon.KartAddon
import net.craftventure.core.feature.kart.inputcontroller.KartController
import net.craftventure.core.feature.kart.physicscontroller.PhysicsController
import net.craftventure.core.ktx.extension.force
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.manager.PlayerStateManager.withGameState
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.metadata.setLeaveLocation
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.physics.ExternalRigidBody
import net.craftventure.core.ride.RotationFixer
import net.craftventure.core.ride.TeleportInterpolationFixer
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import net.craftventure.core.serverevent.PacketUseEntityEvent
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.EntityUtils.setInstantUpdate
import net.craftventure.core.utils.spawnParticleX
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent


class Kart(
    val player: Player,
    val controller: KartController,
    val properties: KartProperties,
    val kartOwner: KartOwner,
    val startLocation: Location,
    val debugBoundingBox: Boolean = false,
    val parkable: Boolean = false,
    val physicsController: PhysicsController = properties.physicsController.getConstructor().newInstance(),
    private val addons: MutableSet<KartAddon> = properties.addons.toMutableSet()
) : Listener {
    protected val armature = properties.clonedArmature
    protected val matrix = Matrix4x4()
    private val interactor = PhysicsInteractor(this)

    //    private var kartEntity: ArmorStand
    internal var location: Vector
        get() = matrix.toVector()
        set(value) {
            matrix.translate(value)
        }
    internal var lastLocation: Vector = location

    //        get() = matrix.rotationYaw
    //        set(value) {
//            matrix.rotateYawPitchRoll(0f, (value - currentYaw).toFloat(), 0f)
//        }
    internal var currentSpeed = 0.0
    internal var currentlyOnGround = false
    private var isDestroying = false

    fun getBukkitLocation(): Location = matrix.toVector().toLocation(world).apply {
        this.yaw = matrix.rotationYaw.toFloat()
    }

    var seats = arrayOf<Seat>()
    var wheels = arrayOf<Wheel>()
    var metadata = hashMapOf<String, Attachment>()

    internal val offset = Vector()
    internal val velocity = Vector()
    internal val newVelocity = Vector()
    internal var world = player.location.world
    var lastInput = System.currentTimeMillis()
        private set

    var invalidated = false
    var destroyed = false
        private set

    var areaTrackerArea = SimpleArea(
        world.name,
        location.x - 48.0,
        location.y - 48.0,
        location.z - 48.0,
        location.x + 48.0,
        location.y + 48.0,
        location.z + 48.0,
    )
        private set
    private var playerTracker: NpcAreaTracker? = null
    var npcs = arrayOf<VisualFakeSeat>()

    private var settingUp: Boolean = false

    @Deprecated("Will be removed after physics externalisation")
    private val velocity2 = Vector()

    private val body = ExternalRigidBody()

    private fun updateTrackerArea() {
        areaTrackerArea = SimpleArea(
            world,
            location.x - 48.0,
            location.y - 48.0,
            location.z - 48.0,
            location.x + 48.0,
            location.y + 48.0,
            location.z + 48.0,
        )
        playerTracker?.updateArea(areaTrackerArea)
    }

    fun start() {
        settingUp = true

        playerTracker = NpcAreaTracker(areaTrackerArea)
        playerTracker!!.startTracking()
//        Logger.console("Kart for ${player.name} invalidated=$invalidated destroyed=$destroyed hash=${hashCode()}")
        seats = properties.seats.map { Seat(it, playerTracker!!) }.toTypedArray()
//        Logger.console("Created ${seats.size} seats")

        matrix.translate(startLocation.x, startLocation.y, startLocation.z)
        body.angle = Math.toRadians(startLocation.yaw.toDouble())
//        currentYaw = startLocation.yaw.toDouble()

        physicsController.create(this)

        if (properties.kartNpcs.isNotEmpty()) {
            npcs = properties.kartNpcs
                .map { VisualFakeSeat(it, playerTracker!!) }
                .toTypedArray()
        }
        wheels = properties.wheels
//            .filter { it.model != null }
            .map { Wheel(it, playerTracker!!) }
            .toTypedArray()

//        Logger.console("Is player in kart? ${player.isInsideVehicle} name=${player.vehicle?.name}")
//        body.angle = Math.toRadians(currentYaw)

        if (properties.leftClickAction is KartAddon) {
            addons.add(properties.leftClickAction)
        }
        if (properties.rightClickAction is KartAddon) {
            addons.add(properties.rightClickAction)
        }

        addons.forEach { it.onStart(this) }
        update()

//        Logger.console("Kart started for ${player.name}")
        controller.start(this)
        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance())
        seats[0].entity!!.addPassenger(player)

//        if (newPhysics) {
//            player.sendMessage(ChatColor.RED + "This kart is using the experimental physics model")
//            velocity.y = 1.0
//        }
        settingUp = false
    }

    fun invalidate() {
        invalidated = true
    }

    fun isParked(): Boolean {
        if (parkable) {
            return player.vehicle == null || player.vehicle !== seats.firstOrNull()?.entity
        }
        return false
    }

    fun isValid(): Boolean {
        if (destroyed) {
//            Logger.debug("Kart invalid reason: Kart destroyed")
            return false
        }
        if (player.isDead) {
//            Logger.debug("Kart invalid reason: Player dead")
            return false
        }
        if (invalidated) {
//            Logger.debug("Kart invalid reason: Invalidated")
            return false
        }
        if (!player.isConnected()) {
//            Logger.debug("Kart invalid reason: Not connected")
            return false
        }
        if (!properties.handling.allowUnderwater.force && seats.any { it.entity?.getFirstPassenger()?.isInWater == true }) {
//            Logger.debug("Passenger is in water")
            return false
        }
        if (!parkable && !seats.any { it.entity == player.vehicle }) {
//            Logger.debug("Kart invalid reason: Not in vehicle")
            return false
        }
        if (!properties.handling.allowUnderwater.force && seats.any { it.isInWater }) {
//            Logger.debug("Kart is in water")
            return false
        }

        if (isParked() && !parkable) {
//            Logger.debug("Kart parked but not parkable")
            return false
        }

        if (controller.isDismounting() && (kartOwner == null || kartOwner.tryToExit(this))) {
            if (!properties.type.isFlying) {
//                Logger.debug("Kart of type ${properties.type} parked=${isParked()} parkable=$parkable")
                if (!parkable) {
//                    Logger.debug("Kart invalid reason: Trying to exit")
                    return false
                } else if (!isParked()) {
                    if (player.vehicle != null) {
//                        Logger.debug("Leaving vehicle")
                        player.leaveVehicle()
                        controller.resetValues()
                    }
                    return true
                }
            }
        }

        return true
    }

    fun update(): Boolean {
        if (!isValid()) {
//            Logger.console("Could not update, invalid!")
            return false
        }

//        Logger.debug("Yaw=${currentYaw.format(2)} ${body.angle.format(2)}")

        if (isParked()) {
            controller.resetValues()
        }

        addons.forEach { it.onPreUpdate(this) }
//        val oldSpeed = currentSpeed

        physicsController.updatePhysics(this, body, interactor)
        velocity2.set(body.velocity.x, velocity.y, body.velocity.y)

        val skidParticlesAreRubber = interactor.skidParticlesAreRubber
        val showSkidParticles = interactor.showSkidParticles

        offset.reset()

        val result =
            BoundingBoxPhysics.move(
                properties.boundingBox,
                world,
                location,
                velocity2,
                properties.handling.maxClimb.force
            )
        if (result != null) {
            offset.set(result.actualMoved)
            currentlyOnGround = result.currentlyOnGround || wheels.any { it.isConsideredTouchingGround }
        } else {
            currentlyOnGround = false
        }
        val currentClimb = result?.currentClimb ?: 0.0

        velocity2.set(offset)
        newVelocity.set(velocity2)
        body.velocity.set(offset.x, offset.z)

        velocity.set(newVelocity)

//        Logger.info("%.2f %.2f %.2f", false, velocity.x, velocity.y, velocity.z)


        val translate = matrix.toVector()
        matrix.setIdentity()
        matrix.translate(translate)
        matrix.translate(0.0, currentClimb, 0.0)
        matrix.translate(velocity)

        if (!CraftventureCore.getSettings().borderFor(player).isInArea(location)) {
            matrix.translate(velocity.clone().multiply(-1))
            currentSpeed = 0.0
            velocity.reset()
            newVelocity.reset()
        }

        matrix.rotateY(-Math.toDegrees(body.angle))

        physicsController.afterMatrix(this, body, interactor, matrix)

        addons.forEach { it.onPreSeatUpdate(this) }
//        val currentYaw = Math.toDegrees(body.angle)
//        val angle = -Math.toRadians(currentYaw)

//        Logger.debug("angle=${currentYaw.format(2)} yaw=${matrix.rotationYaw.format(2)}")

        addons.forEach { it.onPreArmatureUpdate(this, armature, interactor) }
        armature.apply {
            animatedTransform.set(matrix)
            animatedTransform.multiply(transform)
        }
        armature.applyAnimatedTransformsRecursively()
        addons.forEach { it.onPostArmatureUpdate(this, armature, interactor) }

        val calculationMatrix = Matrix4x4()
        for (seat in seats) {
            seat.settings.matrixPreInterceptor?.invoke(this, seat)
            val parentBone = seat.bone ?: seat.settings.parentBone?.let { armature.find(it) ?: armature.byId(it)!! }
                ?.apply { seat.bone = this }
            calculationMatrix.set(parentBone?.animatedTransform ?: matrix)
            calculationMatrix.multiply(seat.settings.matrix)
//            world.spawnParticleX(
//                Particle.END_ROD,
//                calculationMatrix.toVector().x,
//                calculationMatrix.toVector().y + EntityConstants.ArmorStandHeadOffset,
//                calculationMatrix.toVector().z,
//            )
            seat.settings.matrixInterceptor?.invoke(this, calculationMatrix)

            seat.moveTo(world!!, calculationMatrix)
        }

        for (wheel in wheels) {
            val parentBone = wheel.bone ?: wheel.config.source.parentBone?.orElse()
                ?.let { armature.find(it)!! }?.apply { wheel.bone = this }
            calculationMatrix.set(parentBone?.animatedTransform ?: matrix)
            calculationMatrix.multiply(wheel.config.matrix)
//            world.spawnParticleX(
//                Particle.END_ROD,
//                calculationMatrix.toVector().x,
//                calculationMatrix.toVector().y,
//                calculationMatrix.toVector().z,
//            )

            wheel.moveTo(world!!, calculationMatrix, this)

            if (showSkidParticles) {
                if (properties.brakes.brakeParticle != null) {
                    val wheelPosition = wheel.getParticleLocation()
                    val wheelBlock = world!!.getBlockAt(
                        Location(
                            world, wheelPosition.x,
                            (wheelPosition.y - 0.05),
                            wheelPosition.z
                        )
                    )
                    val shouldDisplaySkid = wheelBlock.type != Material.AIR
                    if (!shouldDisplaySkid) continue
                    if (skidParticlesAreRubber && !wheel.config.hasBrakes) continue
                    val particle = if (!wheel.config.forceCustomParticle && !skidParticlesAreRubber) {
                        Particle.BLOCK_CRACK
                    } else properties.brakes.brakeParticle
                    val particleData = if (particle === Particle.BLOCK_CRACK) {
                        wheelBlock.blockData
                    } else null
                    world?.spawnParticleX(
                        particle,
                        wheelPosition.x,
                        wheelPosition.y,
                        wheelPosition.z,
                        data = particleData
                    )
                }
            }
        }

        for (npc in npcs) {
            npc.settings.matrixPreInterceptor?.invoke(this, npc)
            val parentBone =
                npc.bone ?: npc.settings.parentBone?.let {
                    try {
                        armature.find(it) ?: armature.byId(it)!!
                    } catch (e: Exception) {
                        Logger.debug("Failed to find bone $it")
                        null
                    }
                }?.apply { npc.bone = this }
            calculationMatrix.set(parentBone?.animatedTransform ?: matrix)
            calculationMatrix.multiply(npc.settings.matrix)
//            world.spawnParticleX(
//                Particle.END_ROD,
//                calculationMatrix.toVector().x,
//                calculationMatrix.toVector().y,
//                calculationMatrix.toVector().z,
//            )

            npc.settings.matrixInterceptor?.invoke(this, calculationMatrix)

            npc.moveTo(calculationMatrix)
        }

        if (velocity.x != 0.0 || velocity.y != 0.0 || velocity.z != 0.0) {
            if (controller.forward() != 0f || controller.isHandbraking() || controller.sideways() != 0f) {
//                Logger.console("Kart updating because of movement and input")
                lastInput = System.currentTimeMillis()
            }
        }

        if (!isParked())// && CraftventureCore.isTestServer())
            display(
                player,
                Message(
                    id = ChatUtils.ID_RIDE,
                    text = Component.text(
                        if (PluginProvider.isProductionServer())
                            "${CoasterMathUtils.bptToKmh(interactor.speedInBpt).format(0)} km/h"
                        else
                            "${CoasterMathUtils.bptToKmh(interactor.speedInBpt).format(2)} km/h",
                        CVTextColor.subtle
                    ),
                    type = MessageBarManager.Type.SPEEDOMETER,
                    untilMillis = TimeUtils.secondsFromNow(2.0),
                ),
                replace = true,
            )

        updateTrackerArea()

        if (debugBoundingBox) {
            properties.boundingBox.debug(world!!, location)
        }
        addons.forEach { it.onPostUpdate(this) }

        lastLocation = location

        if (AreaConfigManager.getInstance().isKartingBlocked(location)) {
            destroy()
            player.sendMessage(CVTextColor.serverError + "You are not allowed to kart here")
            return false
        }

//        Logger.info("Location x=${location.x} y=${location.y} z=${location.z}")
        return true
    }

    internal fun destroy() {
//        Logger.debug("Destroying")
//        Logger.debug(Logger.miniTrace(traceLevel = 10))
        if (destroyed) return
        isDestroying = true
        val parked = isParked()
        physicsController.destroy(this)
//        Logger.info("Destroy")
        val playerLocation = player.location
        val location = location.toLocation(player.world)
//        Logger.debug(
//            "Exiting ${player.name} to x=${location.x.format(2)} y=${location.y.format(2)} z=${location.z.format(
//                2
//            )}"
//        )
        location.yaw = playerLocation.yaw
        location.pitch = playerLocation.pitch
        if (!parked) {
            player.setLeaveLocation(location)
//            EntityUtils.teleport(player, location)
        }
//        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)

        controller.stop()
        kartOwner.onDestroyed(this)
        playerTracker?.release()
        for (seat in seats) {
            seat.destroy()
        }
        wheels.forEach { it.destroy(this) }
        HandlerList.unregisterAll(this)
        addons.forEach { it.onDestroy(this) }
        metadata.forEach { t, u ->
            u.onDestroy()
        }
        player.withGameState {
            if (it.kart === this)
                it.kart = null
        }
        destroyed = true
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (isParked()) return
        if (event.player == player) {
            event.isCancelled = true
//            Logger.console("Kart drop")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (isParked()) return
        for (seat in seats) {
            if (seat.entity == event.rightClicked) {
                event.isCancelled = true
                return
            }
        }
        for (wheel in wheels) {
            if (wheel.entity?.entityId == event.rightClicked.entityId) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (event.player.isInsideVehicle || event.player.isSneaking) return
        val firstSeat = seats.firstOrNull()
        if (firstSeat != null && (event.rightClicked === firstSeat.entity ||
                    (event.player == player && seats.any { it.entity?.entityId == event.rightClicked.entityId } && firstSeat.entity?.hasPassengers() == false))
        ) {
            if (event.player === player) {
                controller.resetValues()
                firstSeat.entity?.addPassenger(event.player)
            } else {
                if (event.player.isVIP()) {
                    event.player.sendMessage(CVTextColor.serverNotice + "This kart is parked here by ${player.name}")
                } else {
                    event.player.sendMessage(CVTextColor.serverNotice + "This kart is parked here by ${if (player.isVIP()) "VIP " else ""}${player.name}. Only VIP's can spawn and buy karts")
                }
            }
            event.isCancelled = true
            return
        }
//        if (isParked()) return
        if (handleClick(event.rightClicked.entityId, event.player)) {
            event.isCancelled = true
        } else if (event.player === player && !isParked()) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
//        if (isParked()) return
        if (handleClick(event.rightClicked.entityId, event.player)) {
            event.isCancelled = true
        }
        if (event.player === player && !isParked()) {
            event.isCancelled = true
            (event.rightClicked as? Player)?.let { executeLeftAction(it) }
        }
    }

    fun executeLeftAction(player: Player) {
        properties.leftClickAction?.execute(this, KartAction.Type.LEFT_CLICK, player)
    }

    fun executeRightAction(player: Player) {
        properties.rightClickAction?.execute(this, KartAction.Type.RIGHT_CLICK, player)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        if (isParked()) return
        if (event.damager === player) {
            event.isCancelled = true
            executeLeftAction(player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (isParked()) return
        if (event.player === player) {
            event.isCancelled = true
//            Logger.info("Interact type ${event.action}")
            if (event.action === Action.LEFT_CLICK_AIR || event.action === Action.LEFT_CLICK_BLOCK) {
                executeLeftAction(event.player)
            } else {
                executeRightAction(event.player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSpectatorTargetChanged(event: PlayerStartSpectatingEntityEvent) {
//        if (isParked()) return
        onDismount(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDismountEvent(event: EntityDismountEvent) {
//        Logger.debug("Dismount ${event.entity === player} ${isParked()} seat=${seats.any { it.entity == event.dismounted }}")
        if (event.entity === player && isParked()) {
            if (seats.any { it.entity == event.dismounted }) {
                player.withGameState {
                    it.kart = null
                }

                player.removePotionEffect(PotionEffectType.INVISIBILITY)
                if (!isDestroying)
                    player.sendMessage(CVTextColor.serverNotice + "You parked your kart, use /leave to destroy it")
                properties.exitHandler?.onExit(this, player)
            }
            return
        }
        if (event.entity is Player) {
            if (!onDismount(event.entity as Player)) {
//                event.isCancelled = true
//                event.dismounted.addPassenger(event.entity)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onHandleWornItems(event: PlayerEquippedItemsUpdateEvent) {
        if (event.player !== player || isParked()) return
//        if (kartOwner.isOwner(player)) {
        kartOwner.handlePlayerWornItemUpdateEvent(event)
//        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (isParked()) return
        if (handleClick(event.rightClicked.entityId, event.player)) {
            event.isCancelled = true
        }
    }

    private fun onDismount(player: Player): Boolean {
//        Logger.debug("Handle dismount")
        for (seat in seats) {
            val passengers = seat.entity?.passengers
//            Logger.debug("Passengers ${passengers}")
            if (passengers != null && player in passengers) {
                val ownerAllowsLeave = kartOwner == null || kartOwner.canExit(this)
                val kartAllowsLeave = (player !== this.player)
                val canExit = kartAllowsLeave && ownerAllowsLeave
//                Logger.debug("${player.name} dismounting $canExit")
                if (canExit) {
                    properties.exitHandler?.onExit(this, player)
                }
                return canExit
            }
        }
        return true
    }

    @EventHandler
    fun onKartMount(event: EntityMountEvent) {
        val player = event.entity as? Player ?: return
        if (isParked()) {
            if (player === this.player && seats.none { it.entity === event.mount }) {
                destroy()
            }
            return
        }

        seats.firstOrNull { it.settings.passengerSeat }?.let { seat ->
            if (seat.entity === event.mount) {
                if (player === this.player) {
                    player.withGameState {
                        it.kart = this
                    }
                }
                if (player === this.player && seat.settings.shouldPlayerBeInvisible) {
                    player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.INVISIBILITY,
                            Int.MAX_VALUE,
                            1,
                            true,
                            false,
                            false
                        )
                    )
                    EquipmentManager.reapply(player)
                }
                if (properties.type.isFlying && player === this.player) {
                    display(
                        player,
                        Message(
                            id = ChatUtils.ID_KART_EXIT_WARNING,
                            text = Component.text(
                                "You have to leave this kart using /leave",
                                CVTextColor.serverNotice
                            ),
                            type = MessageBarManager.Type.KART_EXIT,
                            untilMillis = TimeUtils.secondsFromNow(5.0),
                        ),
                        replace = true,
                    )
                }
                return
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPacketUseEntity(event: PacketUseEntityEvent) {
        val entity = npcs.firstOrNull { it.entity?.entityId == event.interactedEntityId }
        if (entity != null) {
            val origin = entity.entity?.getLocation() ?: return
            val nearest =
                seats.minByOrNull { it.entity?.location?.distanceSquared(origin) ?: Double.MAX_VALUE } ?: return
            if (nearest.entity!!.location.distance(origin) < 0.2) {
//                event.newInteractedEntityId = nearest.entity!!.entityId
            } else
                event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (settingUp) return
        if (isParked()) return
        if (event.player === player) {
//            Logger.info("Teleport ${event.player.name} with reason ${event.cause}")
            val seat = seats.firstOrNull { it.entity?.passengers?.contains(player) ?: false }
            if (seat != null && seat.destroyed)
                return
            if (destroyed)
                return

            if (kartOwner.canExit(this) != false) {
                if (!parkable)
                    destroy()
//            Logger.info("Teleport cancelled!")

            } else {
                event.isCancelled = true
            }
        }
    }

    // TODO: Use this method in PlayerStateManagemer once entering is correctly handled by state
    fun tryParkOrDestroy() = tryPark() || requestDestroy()

    fun tryPark(): Boolean {
        if (isParked()) return true
        if (parkable) {
            return player.leaveVehicle()
        }

        return false
    }

    fun requestDestroy(): Boolean {
        if (kartOwner.allowUserDestroying()) {
            destroy()
            return true
        }
        return false
    }

    private fun handleClick(clickedId: Int, player: Player): Boolean {
        for (seat in seats) {
            if (clickedId == seat.entity?.entityId) {
                if (player.isInsideVehicle || player.isSneaking) return true
                val entity = seat.entity
                if (entity != null && seat.settings.passengerSeat && !EntityUtils.hasPlayerPassengers(entity)) {
//                    Logger.debug("${player.name} trying to enter kart with permission ${seat.settings.enterPermission}=${seat.settings.enterPermission.let { it == null || player.hasPermission(it) }}")
                    if (seat.settings.enterPermission.let { it == null || player.hasPermission(it) }) {
                        entity.addPassenger(player)
                        return true
                    }
                }
                return true
            }
        }
        return false
    }

    inner class Seat(var settings: KartSeat, val tracker: NpcEntityTracker) {
        var entity: Entity? = null
        var destroyed = false
        var currentMatrix = Matrix4x4()
            private set
        var bone: Joint? = null
//        private val rotationFixer = RotationFixer()

        val isInWater: Boolean
            get() = entity != null && entity!!.isInWater

        fun moveTo(world: World, matrix: Matrix4x4) {
            currentMatrix.set(matrix)
            if (destroyed) return
            val position = matrix.toVector()
            val x = position.x
            val y = position.y - EntityConstants.ArmorStandHeadOffset
            val z = position.z

            val rotation = matrix.rotation
            val yaw = rotation.yaw.toFloat()

            if (entity == null || entity?.isValid != true) {
//                Logger.console("Spawning kart seat")
                entity = world.spawnEntity(
                    Location(world, x, y, z, yaw, 0f),
                    settings.entityType
                ).apply {
                    setInstantUpdate()
                    isPersistent = false
                    if (this is LivingEntity)
                        setAI(false)
                    isInvulnerable = true
                    customName = "kart"
//                    EntityUtils.noClip(this, true)
                    isSilent = true
                    if (this is LivingEntity)
                        this.isInvisible = true
                    this.setOwner(kart = this@Kart)
                    this.setGravity(false)
                }

            } else {
                val seat = entity
//                Logger.info("Moving to yaw ${(yaw + settings.yaw).format(2)} on thread ${Thread.currentThread().name} ${CraftventureCore.isOnMainThread()}")
                if (seat is LivingEntity && seat !is ArmorStand) {
                    EntityUtils.teleportWithHeadYaw(entity!!, x, y, z, yaw, 0f)
                } else {
                    EntityUtils.teleport(entity!!, x, y, z, yaw, 0f)
                }
            }

//            entity?.passengers?.forEach {
//                if (it is Player) {
//                    SmoothCoastersHelper.api.setRotation(
//                        null,
//                        player,
//                        rotation.x.toFloat(),
//                        rotation.y.toFloat(),
//                        rotation.z.toFloat(),
//                        rotation.w.toFloat(),
//                        1,
//                    )
//                }
//            }

//            (entity as? ArmorStand)?.let { entity ->
//                rotationFixer.setNextRotation(matrix.rotation)
//                val fixedRotation = rotationFixer.getCurrentRotation()
//                entity.headPose = fixedRotation.toEuler()
//            }
        }

        fun destroy() {
            destroyed = true
            entity?.apply {
                passengers.forEach { it.leaveVehicle() }
            }
            entity?.remove()
        }
    }

    class PhysicsInteractor(val kart: Kart) {
        var speedInBpt: Double = 0.0
        var skidParticlesAreRubber = true
        var showSkidParticles = false
        val body get() = kart.body
        val matrix get() = kart.matrix
    }

    class Wheel(var config: KartProperties.WheelConfig, val tracker: NpcEntityTracker) {
        var entity: NpcEntity? = null
        var destroyed = false
        var angle = 0.0
        var currentMatrix = Matrix4x4()
            private set
        var bone: Joint? = null

        var teleportFixer: TeleportInterpolationFixer? = null

        var isConsideredTouchingGround = false
            private set

        fun getParticleLocation() =
            teleportFixer?.getCurrentPosition() ?: entity?.getLocation()?.toVector() ?: currentMatrix.toVector()

        fun moveTo(world: World, matrix: Matrix4x4, kart: Kart) {
            currentMatrix.set(matrix)
            val position = matrix.toVector()

            if (teleportFixer == null) {
                teleportFixer = TeleportInterpolationFixer()
            }
            teleportFixer!!.setNextLocation(position)

            isConsideredTouchingGround =
                world.getBlockAt(position.x.toInt(), position.y.toInt(), position.z.toInt()).isSolid ||
                        world.getBlockAt(position.x.toInt(), (position.y - 0.95).toInt(), position.z.toInt()).isSolid

            val model = config.model
            if (model == null) {
//                Logger.debug("No model")
                return
            }
            val circumference = config.circumference
            if (circumference == null) {
//                Logger.debug("No circum")
                return
            }
            val radius = config.radius!!
            val diameter = config.diameter!!

            val x = position.x
            val y = position.y - EntityConstants.ArmorStandHeadOffset + radius
            val z = position.z
            val rotation = matrix.rotation
            //config.baseYaw - (kart.controller.sideways() * 20 * config.steerInfluence).toFloat()
            val steerOffset =
                if (config.isSteered) kart.controller.sideways() * (config.steerAngle?.toFloat() ?: 20f)
                else 0f
            val yaw: Float = rotation.yaw.toFloat() + steerOffset
            val pitch: Float = rotation.pitch.toFloat()
            if (kart.currentSpeed != 0.0)
                angle += (kart.currentSpeed / circumference) * Math.PI * 2
//            Logger.debug("Angle ${angle.format(2)} currentSpeed=${kart.currentSpeed.format(2)} circum=${config.circumference.format(2)}")

            if (entity == null) {
                entity = NpcEntity(
                    "kart",
                    EntityType.ARMOR_STAND, Location(
                        Bukkit.getWorld("world"),
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                    )
                )

                entity!!.apply {
                    helmet(model)
                    invisible(true)
                    noGravity(true)
//                    forceTeleport = true
                }
                tracker.addEntity(entity!!)

//                if (CraftventureCore.isTestServer()) {
//                    entity!!.passengers += entity!!.location.spawn<Chicken>().apply {
//                        EntityUtils.invisible(this, true)
//                        customName = "kart"
//                    }
//                }
            } else {
                entity!!.apply {
//                    customNameVisible(true)
//                    customName("${x.format(3)}/${y.format(3)}/${z.format(3)} with ${yaw.format(3)}/${pitch.format(3)}")
                    if (this.entityType == EntityType.ARMOR_STAND)
                        head(Math.toDegrees(if (config.isLeftSide) angle else -angle).toFloat(), 0f, 0f)
                    move(x, y, z, yaw, pitch, yaw)
                }
            }
        }

        fun destroy(kart: Kart) {
            destroyed = true
            if (entity != null)
                tracker.removeEntity(entity!!)
        }
    }

    class VisualFakeSeat(var settings: KartNpc, val tracker: NpcEntityTracker) {
        var entity: NpcEntity? = null
        var destroyed = false
        var currentMatrix = Matrix4x4()
            private set
        var bone: Joint? = null
        private val rotationFixer = RotationFixer()

        fun moveTo(matrix: Matrix4x4) {
            currentMatrix.set(matrix)
            val position = matrix.toVector()
            val x = position.x
            val y = position.y - EntityConstants.ArmorStandHeadOffset
            val z = position.z

            if (entity == null) {
                entity = NpcEntity(
                    "kartSeat",
                    settings.entityType, Location(
                        Bukkit.getWorld("world"),
                        x,
                        y,
                        z,
                    ), settings.cachedGameProfile
                )
                entity!!.apply {
                    helmet(settings.model)
                    if (settings.entityType == EntityType.ARMOR_STAND)
                        invisible(true)
                    forceTeleport = true
                }
                tracker.addEntity(entity!!)
            } else {
                entity!!.apply {
//                    customNameVisible(true)
//                    customName("${x.format(3)}/${y.format(3)}/${z.format(3)} with ${yaw.format(3)}/${pitch.format(3)}")
                    if (this.entityType == EntityType.ARMOR_STAND && settings.useHeadRotation) {
                        val rotation = matrix.rotation
//                        val pose = TransformUtils.getArmorStandPose(rotation)
//                        head(pose.x.toFloat(), pose.y.toFloat(), pose.z.toFloat())
                        rotationFixer.setNextRotation(rotation)
                        val fixedRotation = rotationFixer.getCurrentRotation()
                        head(fixedRotation.x.toFloat(), fixedRotation.y.toFloat(), fixedRotation.z.toFloat())
                        move(x, y, z, 0f, 0f, 0f)
                    } else {
                        val rotation = matrix.rotation
                        val yaw = if (settings.useHeadRotation) 0f else rotation.yaw.toFloat()
                        val pitch = if (settings.useHeadRotation) 0f else rotation.pitch.toFloat()
                        move(x, y, z, yaw, pitch, yaw)
                    }

                }
            }
        }

        fun destroy() {
            destroyed = true
            if (entity != null)
                tracker.removeEntity(entity!!)
        }
    }

    interface Attachment {
        fun onDestroy()
    }
}