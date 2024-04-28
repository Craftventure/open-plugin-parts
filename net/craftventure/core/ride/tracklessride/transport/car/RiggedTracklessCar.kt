package net.craftventure.core.ride.tracklessride.transport.car

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.entitymeta.EntityEvents.addListener
import net.craftventure.bukkit.ktx.extension.asString
import net.craftventure.bukkit.ktx.extension.setColor
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.BukkitColorUtils
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.animation.armature.ArmatureAnimator
import net.craftventure.core.animation.armature.WrappedJoint
import net.craftventure.core.animation.dae.DaeLoader
import net.craftventure.core.async.executeMain
import net.craftventure.core.extension.hasPassengers
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.ride.RotationFixer
import net.craftventure.core.ride.tracklessride.BaseTagContainer
import net.craftventure.core.ride.tracklessride.TagContainer
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.config.CarGroupCarConfig
import net.craftventure.core.ride.tracklessride.config.CarGroupConfig
import net.craftventure.core.ride.tracklessride.navigation.PathPosition
import net.craftventure.core.ride.tracklessride.property.BooleanProperty
import net.craftventure.core.ride.tracklessride.property.DoubleProperty
import net.craftventure.core.serverevent.PacketUseEntityEvent
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.EntityUtils.setInstantUpdate
import net.craftventure.core.utils.ItemStackUtils
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class RiggedTracklessCar(
    position: PathPosition,
    idInGroup: Int,
    tracklessRide: TracklessRide,
    tagContainer: TagContainer = BaseTagContainer(),
    armatures: Array<Armature>,
    val config: Config,
    val models: Map<String, ItemStack?>,
    groupConfig: CarGroupConfig,
    carConfig: CarGroupCarConfig,
) : TracklessRideCar(
    position = position,
    idInGroup = idInGroup,
    tracklessRide = tracklessRide,
    tagContainer = tagContainer,
    groupConfig = groupConfig,
    carConfig = carConfig,
) {
    private val listener = object : Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        fun onPacketUseEntityEvent(event: PacketUseEntityEvent) {
//            Logger.debug("Clicked ${event.interactedEntityId}")
            val bone = npcs.firstOrNull { it.npc.entityId == event.interactedEntityId } ?: return
//            Logger.debug("Linked to ${bone.joint.joint.joint.name}")
            val replacement = seats.firstOrNull { it.joint.joint.joint.id == bone.joint.joint.joint.id } ?: return
//            Logger.debug("To seat ${replacement.joint.joint.joint.name} with it ${replacement.entity?.entityId}")
            event.isCancelled = true
            val entityId = replacement.entity?.entityId
            if (entityId != null)
                executeMain { tracklessRide.handleInteract(event.player, this@RiggedTracklessCar, entityId) }
//            event.newInteractedEntityId = replacement.entity?.entityId
        }

        @EventHandler(ignoreCancelled = true)
        fun onWornItemsUpdate(event: PlayerEquippedItemsUpdateEvent) {
            val team = team ?: return
            val shooterRideContext = tracklessRide.shooterRideContext ?: return
            val player = event.player
            if (player !in team) return
            event.appliedEquippedItems.weaponItem = shooterRideContext.gunItem
//                ItemStackUtils.fromString(shooterRideContext.config.gunItem)!!.toCachedItem("shooter_ride_gun")
        }
    }
    private val matrix = Matrix4x4()
    private val armatureAnimators = armatures.map { ArmatureAnimator(it) }
    private val world = Bukkit.getWorld("world")!!
    private val area: SimpleArea = position.location.let { center ->
        SimpleArea(
            world.name,
            center.x - 48.0,
            center.y - 48.0,
            center.z - 48.0,
            center.x + 48.0,
            center.y + 48.0,
            center.z + 48.0,
        )
    }
    private val areaTracker: NpcAreaTracker = NpcAreaTracker(area)

    private val joints = armatures.flatMap {
        it.allJoints().map { joint ->
            ManagedJoint(WrappedJoint(joint), config.bones[joint.name])
        }
    }

    private val doubleProperties: MutableMap<String, DoubleProperty> = joints.flatMap {
        listOf(it.pitch, it.yaw, it.roll)
    }.associateBy { it.id }.toMutableMap()

    private val booleanProperties: MutableMap<String, BooleanProperty> = mutableMapOf<String, BooleanProperty>().apply {
        this["follow_track_heading"] = BooleanProperty("follow_track_heading", true)
    }

    private val seats = config.bones.filter { it.value.isSeat }.map { entry ->
        val joint = joints.first { it.joint.joint.name == entry.key }
        ManagedSeat(entry.value, joint)
    }

    private val npcs = config.bones.filter { !it.value.isSeat }.map { entry ->
        val joint = joints.first { it.joint.joint.name == entry.key }
        val modelItem = models[entry.key]!!
        val npc = NpcEntity("riggedTracklessCar", EntityType.ARMOR_STAND, pathPosition.location.toLocation(world))
        npc.invisible(true)
        if (entry.value.isSmall) {
            npc.armorstandSmall(true)
        }
        npc.helmet(modelItem)
        areaTracker.addEntity(npc)
        ManagedNpc(entry.value, npc, modelItem, joint)
    }

    init {
//        logcat { "With models ${models.entries.joinToString { "${it.key}=${it.value?.type}" }}" }
        Bukkit.getPluginManager().registerEvents(listener, PluginProvider.getInstance())

        val colors = hashMapOf<String, String>()
        colors.putAll(groupConfig.colors)
        colors.putAll(carConfig.colors)

        colors.forEach { (jointId, color) ->
            val seats = seats.filter { it.joint.joint.joint.name == jointId }
            val npcs = npcs.filter { it.joint.joint.joint.name == jointId }

            try {
                val parsedColor = BukkitColorUtils.parseColor(color)
                seats.forEach { seat ->
                    seat.color = parsedColor
                }
                npcs.forEach { npc ->
                    npc.color = parsedColor
                }
//                Logger.debug(
//                    "${parsedColor} for seats=${seats.size} npcs=${npcs.size} with id ${jointId} of ids ${
//                        joints.joinToString(
//                            ", "
//                        ) { it.joint.joint.id }
//                    }")
            } catch (e: Exception) {
                Logger.warn("Failed to parse color $color for TracklessRide ${tracklessRide.id} group=${group.groupId} carId=$idInGroup")
            }
        }
        update()
        areaTracker.startTracking()
    }

    override fun tryToEnter(player: Player, seatEntityId: Int): Boolean {
//        Logger.debug("Try to enter")
        val seat = seats.firstOrNull { it.entity?.entityId == seatEntityId } ?: return false
//        Logger.debug("Seat found")
        val seatEntity = seat.entity ?: return false
//        Logger.debug("Entity found")
        if (seatEntity.hasPassengers()) return false
//        Logger.debug("No passengers")
        seatEntity.addPassenger(player)
        return true
    }

    override fun putPassenger(player: Player): Boolean {
        if (!canEnter) return false
        seats.forEach { seat ->
            val entity = seat.entity ?: return@forEach
            if (entity.hasPassengers()) return@forEach
            player.teleport(entity)
            return entity.addPassenger(player)
        }
        return false
    }

    override fun getDoubleProperty(id: String): DoubleProperty? {
        return doubleProperties[id] ?: super.getDoubleProperty(id)
    }

    override fun getBooleanProperty(id: String): BooleanProperty? {
        return booleanProperties[id] ?: super.getBooleanProperty(id)
    }

    override fun destroy() {
        super.destroy()
        HandlerList.unregisterAll(listener)
        seats.forEach {
            it.destroy()
        }
        areaTracker.stopTracking()
        areaTracker.release()
    }

    override fun hasPlayers(): Boolean {
        return seats.any { it.entity?.hasPassengers() == true }
    }

    override val playerPassengers: Collection<Player>
        get() = seats.flatMap {
            it.entity?.passengers?.filterIsInstance<Player>() ?: emptyList()
        }

    private fun updateTrackerArea() {
        val location = pathPosition.location
        area.loc1.set(
            location.x - 48.0,
            location.y - 48.0,
            location.z - 48.0,
        )
        area.loc2.set(
            location.x + 48.0,
            location.y + 48.0,
            location.z + 48.0,
        )
    }

    override fun update() {
        super.update()
        updateTrackerArea()

        matrix.setIdentity()
        matrix.translate(pathPosition.location)

        joints.forEach { joint ->
            joint.joint.reset()
//            Logger.debug(
//                "Joint ${joint.joint.joint.name} ${joint.pitch.value.format(2)} ${joint.yaw.value.format(2)} ${
//                    joint.roll.value.format(
//                        2
//                    )
//                }"
//            )
            joint.joint.joint.transform.rotateYawPitchRoll(
                joint.pitch.value,
                joint.yaw.value,
                joint.roll.value
            )
        }

        armatureAnimators.forEach { animator ->
            animator.armature.apply {
                animatedTransform.set(matrix)
                animatedTransform.multiply(transform)
                applyAnimatedTransformsRecursively()
            }

//            animator.allJoints.forEach {
//                val location = it.animatedTransform.toVector().toLocation(world)
//                location.spawnParticleX(Particle.END_ROD)
//            }
        }

        npcs.forEach { npc ->
            val position = npc.joint.joint.joint.animatedTransform.toVector()
            val x = position.x
            val y = position.y - EntityConstants.ArmorStandHeadOffset
            val z = position.z

            npc.rotationFixer.setNextRotation(npc.joint.joint.joint.animatedTransform.rotation)
            val fixedRotation = npc.rotationFixer.getCurrentRotation()
//            Logger.debug("${npc.joint.joint.joint.name} ${npc.joint.yaw.value.format(2)}")
//            val debug = npc.joint.joint.joint.animatedTransform.rotation.yawPitchRoll
//            if (idInGroup == 1 && npc.joint.joint.joint.name == "seat")
//                Logger.debug(
//                    "Npc ${npc.joint.joint.joint.name} " +
//                            "${npc.joint.yaw.value.format(10)} " +
//                            "ry=${npc.joint.joint.joint.animatedTransform.rotationYaw.format(2)} " +
//                            "fy=${fixedRotation.y.toFloat().format(2)} " +
//                            "r=${fixedRotation.x.format(10)} " +
//                            "r=${fixedRotation.y.format(10)} " +
//                            "r=${fixedRotation.z.format(10)} " +
//                            "d=${debug.x.format(10)} " +
//                            "d=${debug.y.format(10)} " +
//                            "d=${debug.z.format(10)} " +
////                            "${npc.config.usePositionYaw} "
//                            ""
//                )

            npc.npc.move(
                x,
                y,
                z,
                if (npc.config.usePositionYaw) npc.joint.yaw.value.toFloat() else 0f,
                0f,
                0f
            )
            npc.npc.head(
                fixedRotation.x.toFloat(),
                if (npc.config.usePositionYaw) 0f else fixedRotation.y.toFloat(),
                fixedRotation.z.toFloat()
            )
        }
        seats.forEach { it.update() }
    }

    inner class ManagedSeat(
        val config: BoneConfig,
        val joint: ManagedJoint,
        color: Color? = null,
    ) {
        var entity: ArmorStand? = null
            private set
        var color = color
            set(value) {
                field = value
                entity?.remove()
            }

        //        private val rotationFixer = RotationFixer()
        fun update() {
            val position = joint.joint.joint.animatedTransform.toVector()

            if (position !in tracklessRide.area) {
                logcat(priority = LogPriority.WARN) { "For ${tracklessRide.displayName()} car=${idInGroup} at ${position.asString()} is out of area bounds" }
            }
//            val x = position.x
//            val y = position.y - EntityConstants.ArmorStandHeadOffset
//            val z = position.z

//            rotationFixer.setNextRotation(joint.joint.joint.animatedTransform.rotation)
//            val fixedRotation = rotationFixer.getCurrentRotation()

            val location = position.toLocation(world)
            location.y -= EntityConstants.ArmorStandHeadOffset
            location.yaw = joint.yaw.value.toFloat()
            location.pitch = joint.pitch.value.toFloat()
            val entity = entity?.takeIf { it.isValid } ?: location.spawn<ArmorStand>().also { entity ->
                if (this.entity != null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) {
                        "Respawning entity for ${tracklessRide.displayName()} car=${idInGroup}, old one was invalid old=${
                            this.entity?.location?.toVector()?.asString()
                        } new=${location.toVector().asString()}"
                    }
                }
                this.entity?.remove()
                if (config.isSmall)
                    entity.isSmall = true
                entity.customName(Component.text(tracklessRide.id))
                entity.setOwner(tracklessRide)
                entity.equipment.helmet =
                    ItemStackUtils.fromString(config.model)?.let { if (color != null) it.setColor(color!!) else it }
                entity.isVisible = false
                entity.setInstantUpdate()
                entity.addDisabledSlots(*EquipmentSlot.values())
                this.entity = entity
                entity.addListener(createSeatEntityEventListener(tracklessRide, this@RiggedTracklessCar))
            }
            EntityUtils.teleport(entity, location)
//            entity.headPose = fixedRotation.toEuler()
        }

        fun destroy() {
            entity?.remove()
        }
    }

    class ManagedNpc(
        val config: BoneConfig,
        val npc: NpcEntity,
        var itemStack: ItemStack,
        val joint: ManagedJoint,
        val rotationFixer: RotationFixer = RotationFixer(),
        color: Color? = null,
    ) {
        var color = color
            set(value) {
//                logcat { "Updating helmet with color $value same=${npc.helmet === itemStack} color=${npc.helmet?.getColor()}" }
                field = value
                if (value != null)
                    itemStack = itemStack.setColor(value)
                npc.helmet(itemStack)
//                logcat { "same=${npc.helmet === itemStack} ${npc.helmet?.itemMeta == itemStack.itemMeta} color=${npc.helmet?.getColor()} vs ${itemStack.getColor()}" }
            }
    }

    class ManagedJoint(
        val joint: WrappedJoint,
        val config: BoneConfig?,
        val yaw: DoubleProperty = DoubleProperty("${joint.joint.name}/yaw", 0.0),
        val pitch: DoubleProperty = DoubleProperty("${joint.joint.name}/pitch", 0.0),
        val roll: DoubleProperty = DoubleProperty("${joint.joint.name}/roll", 0.0),
    )

    @JsonClass(generateAdapter = true)
    class BoneConfig(
        val model: String?,
        val isSeat: Boolean = false,
        val usePositionYaw: Boolean = false,
        val isSmall: Boolean = false,
    )

    @JsonClass(generateAdapter = true)
    class Config(
        val armatureFile: String,
        val bones: Map<String, BoneConfig>,
    ) : CarConfig() {
        override fun createFactory(directory: File): CarFactory {
            val daeFile = File(directory, "armature/${armatureFile}")
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(daeFile)
            val armatures = DaeLoader.load(doc, daeFile.name)
            return Factory(armatures, this)
        }

        companion object {
            const val type = "rigged"
        }
    }

    class Factory(
        val armatures: Array<Armature>,
        val config: Config,
    ) : CarFactory() {
        override fun produce(
            position: PathPosition,
            groupId: Int,
            groupConfig: CarGroupConfig,
            carId: Int,
            carConfig: CarGroupCarConfig,
            trackedRide: TracklessRide,
            tagContainer: TagContainer
        ): TracklessRideCar = RiggedTracklessCar(
            position,
            carId,
            trackedRide,
            tagContainer,
            armatures.map { it.clone() }.toTypedArray(),
            config,
            config.bones.mapValues { it.value.model?.let { ItemStackUtils.fromString(it) } },
            groupConfig,
            carConfig
        )
    }
}