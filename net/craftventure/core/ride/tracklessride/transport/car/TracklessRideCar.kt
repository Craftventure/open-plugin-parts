package net.craftventure.core.ride.tracklessride.transport.car

import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import net.craftventure.core.ride.tracklessride.BaseTagContainer
import net.craftventure.core.ride.tracklessride.TagContainer
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.config.CarGroupCarConfig
import net.craftventure.core.ride.tracklessride.config.CarGroupConfig
import net.craftventure.core.ride.tracklessride.navigation.PathPosition
import net.craftventure.core.ride.tracklessride.property.BooleanProperty
import net.craftventure.core.ride.tracklessride.property.DoubleProperty
import net.craftventure.core.ride.tracklessride.property.DoubleProperty.Companion.ofDelegate
import net.craftventure.core.ride.tracklessride.property.DoublePropertyAnimator
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.utils.LookAtUtil
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent

abstract class TracklessRideCar(
    position: PathPosition,
    var idInGroup: Int,
    val tracklessRide: TracklessRide,
    val tagContainer: TagContainer = BaseTagContainer(),
    groupConfig: CarGroupConfig,
    carConfig: CarGroupCarConfig,
) : TagContainer by tagContainer {
    val key: String
        get() = "${group.groupId},${idInGroup}"
    var team: ShooterRideContext.Team? = null

    //        set(value) {
//            field = value
//            Logger.debug("Team set to $value")
//        }
    lateinit var group: TracklessRideCarGroup
    protected val activeAnimators = hashMapOf<DoubleProperty, AssignedAnimator<DoublePropertyAnimator>>()

    var currentSpeed: Double = 0.0
    var canEnter = false
    var isEjecting = false
        private set

    var targetSpeed: Double = CoasterMathUtils.kmhToBpt(6.0)
    var targetAcceleration: Double = CoasterMathUtils.kmhToBpt(0.2)
    var targetDeceleration: Double = CoasterMathUtils.kmhToBpt(0.2)

    private val doubleProperties: Map<String, DoubleProperty> = mapOf(
        "targetSpeed" to ofDelegate("targetSpeed", ::targetSpeed),
        "targetSpeedAcceleration" to ofDelegate("targetSpeed", ::targetAcceleration),
        "targetSpeedDeceleration" to ofDelegate("targetSpeed", ::targetDeceleration),
    )

    abstract fun tryToEnter(player: Player, seatEntityId: Int): Boolean
    abstract fun putPassenger(player: Player): Boolean

    @Volatile
    private var lastPathPosition: PathPosition = position

    @Volatile
    var pathPosition: PathPosition = position
        private set

    val lastTrackYaw get() = LookAtUtil.getYawToTarget(lastPathPosition.location, pathPosition.location)

    open fun destroy() {}

    fun isDummy() = this is DummyTracklessRideCar

    open fun update() {
        val iterator = activeAnimators.iterator()

        while (iterator.hasNext()) {
            val (property, assignedAnimator) = iterator.next()
            val finished = assignedAnimator.animator.animate(50, property)
            if (finished) {
                iterator.remove()
            }
        }
    }

    open fun eject() {
        isEjecting = true
        try {
            val passengers = playerPassengers
            passengers.forEach { player ->
                player.leaveVehicle()
            }
        } finally {
            isEjecting = false
        }
    }

    abstract val playerPassengers: Collection<Player>
    abstract fun hasPlayers(): Boolean

    open fun moveTo(pathPosition: PathPosition) {
        this.lastPathPosition = this.pathPosition
        this.pathPosition = pathPosition
    }

    open fun getDoubleProperty(id: String): DoubleProperty? {
        return doubleProperties[id]
    }

    open fun getBooleanProperty(id: String): BooleanProperty? {
        return null
    }

    open fun animateProperty(id: String, animator: DoublePropertyAnimator) {
        val property = getDoubleProperty(id) ?: return
        activeAnimators[property] = AssignedAnimator(animator = animator)
    }

    data class AssignedAnimator<T>(
        val start: Long = System.currentTimeMillis(),
        val animator: T
    )

    companion object {
        fun createSeatEntityEventListener(tracklessRide: TracklessRide, car: TracklessRideCar): Listener {
            return object : Listener {
                @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
                fun onEntityMountEvent(event: EntityMountEvent) {
                    if (event.entity is Player) {
                        tracklessRide.onPlayerEntered(event.entity as Player, car)
                    }
                }

                @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
                fun onEntityDismountEvent(event: EntityDismountEvent) {
                    if (event.entity is Player) {
                        tracklessRide.onPlayerExited(event.entity as Player, car)
                    }
                }

                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
                    tracklessRide.handleInteract(event.player, car, event.rightClicked.entityId)
                }

                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
                    tracklessRide.handleInteract(event.player, car, event.rightClicked.entityId)
                }
            }
        }
    }
}