package net.craftventure.core.ride.trackedride.car.seat

import net.craftventure.core.extension.getFirstPassenger
import net.craftventure.core.extension.hasPassengers
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.ForcedViewManager
import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.TrackedRide
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.MathUtil
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

abstract class Seat<T : Entity>(
    val rightOffset: Double,
    val upOffset: Double,
    val forwardOffset: Double,
    var isPassengerCar: Boolean,
    val entityName: String,
    val permanentYawOffset: Float = 0f
) {
    var isMiniArmorStand: Boolean = false
    var yawOffset: Float = 0.0f
    var modelEulerOffset = Vector()
    var seatPosition = Vector(rightOffset, upOffset, forwardOffset)
    var entity: T? = null
        private set
    private var shouldAlwaysTeleport: Boolean = false
    var permission: String? = null
    var compensateHeight: Double? = null
    var enableFollowCam: Boolean = false
    var invertBanking: Boolean = false
    var moveListener: OnMoveListener<T>? = null

    fun rightOffset() = seatPosition.x
    fun upOffset() = seatPosition.y
    fun forwardOffset() = seatPosition.z

    fun setNoPassengers(): Seat<T> {
        isPassengerCar = false
        return this
    }

    fun putPassenger(entity: Entity): Boolean {
        if (this.entity != null) {
            if (!isPassengerCar)
                return false

            if (!this.entity.hasPassengers()) {
                if (this.entity != null)
                    entity.teleport(this.entity!!)
                return this.entity?.addPassenger(entity) ?: false
            }
        }
        return false
    }

    fun isShouldAlwaysTeleport(): Boolean {
        return shouldAlwaysTeleport
    }

    fun setShouldAlwaysTeleport(shouldAlwaysTeleport: Boolean): Seat<T> {
        this.shouldAlwaysTeleport = shouldAlwaysTeleport
        return this
    }

    fun isEntity(entity: Entity?): Boolean {
        if (entity != null && this.entity != null) {
            if (entity === this.entity)
                return true
            if (entity.entityId == this.entity!!.entityId)
                return true
        }
        return false
    }

    fun isEntity(entityId: Int): Boolean {
        if (this.entity != null) {
            if (entityId == this.entity?.entityId)
                return true
        }
        return false
    }

    val passengers: List<Entity>
        get() = entity?.passengers ?: emptyList()

    fun eject(): Boolean {
        return if (entity != null) entity!!.eject() else false
    }

    fun despawn() {
        if (entity != null)
            entity!!.remove()
    }

    protected open fun applyYaw() = true
    protected open fun applyPitch() = true
    protected open fun applyRoll() = true

    open fun move(
        x: Double,
        y: Double,
        z: Double,
        trackYawRadian: Double,
        trackPitchRadian: Double,
        bankingDegree: Double,
        forceUpdate: Boolean,
        car: RideCar
    ) {
        val banking = if (invertBanking) -bankingDegree else bankingDegree
        val canEnter = car.attachedTrain.canEnter()
        val shouldExist =
            canEnter || shouldAlwaysTeleport || car.velocity == 0.0 || entity?.hasPassengers() == true || true
        val valid = entity?.isValid ?: false
        if (shouldExist && (entity == null || !valid)) {
            entity?.remove()
            entity = spawn(
                Bukkit.getWorlds()[0],
                x,
                y + (compensateHeight ?: 0.0),
                z,
                if (applyYaw()) Math.toDegrees(trackYawRadian) + permanentYawOffset + yawOffset else 0.0,
                if (applyPitch()) Math.toDegrees(trackPitchRadian) else 0.0,
                car
            )
            if (isMiniArmorStand && entity is ArmorStand)
                (entity as ArmorStand).isSmall = true
//            Logger.debug("Spawn")
//            entity?.customName = "spawn"
            entity!!.isPersistent = false
            entity!!.customName = entityName
        } else if (shouldExist && valid) {
            EntityUtils.teleport(
                entity!!,
                x,
                y + (compensateHeight ?: 0.0),
                z,
                if (applyYaw()) (MathUtil.RADTODEG * trackYawRadian).toFloat() + permanentYawOffset + yawOffset else 0.0f,
                if (applyPitch()) (MathUtil.RADTODEG * trackPitchRadian).toFloat() else 0f
            )
//            entity?.customName = "teleport"
//            Logger.debug("Teleport")
        } else if (!shouldExist && valid) {
            entity?.remove()
            entity = null
            Logger.debug("Delete")
        } else {
//            entity?.customName = "nothing"
            Logger.debug("Nothing")
        }

//        entity?.world?.spawnParticle(Particle.END_ROD, x, y, z,
//                1,
//                0.0, 0.0, 0.0,
//                0.0)
//        entity?.world?.spawnParticle(Particle.REDSTONE, x, y + (compensateHeight ?: 0.0), z,
//                1,
//                0.0, 0.0, 0.0,
//                0.0)
        entity?.let { entity ->
            move(entity, x, y, z, trackYawRadian, trackPitchRadian, banking, forceUpdate, car)
        }

//        if (CraftventureCore.isTestServer()) {
//            entity?.customName = entity?.location?.toVector()?.asString(2)
//            entity?.isCustomNameVisible = true
//        }


        if (enableFollowCam && entity != null && entity?.getFirstPassenger() is Player) {
            ForcedViewManager.set(
                entity!!.getFirstPassenger() as Player,
                entity!!.location.yaw,
                Math.toDegrees(trackPitchRadian + (Math.PI * -1.5)).toFloat(),
                90f,
                0.0f,
                10000f
            )
        }

        moveListener?.onMove(this)
    }

    protected abstract fun spawn(
        world: World,
        x: Double,
        y: Double,
        z: Double,
        yaw: Double,
        pitch: Double,
        car: RideCar
    ): T

    protected abstract fun move(
        entity: T,
        x: Double,
        y: Double,
        z: Double,
        trackYawRadian: Double,
        trackPitchRadian: Double,
        bankingDegree: Double,
        forceUpdate: Boolean,
        car: RideCar
    )

    interface OnMoveListener<T : Entity> {
        fun onMove(seat: Seat<T>)
    }

    companion object {
        const val KEY_SEAT = "trackedRideCarSeat"
    }

    abstract class Json {
        var rightOffset: Double = 0.0
        var upOffset: Double = 0.0
        var forwardOffset: Double = 0.0
        var isPassengerCar: Boolean = false
        var permanentYawOffset: Float = 0f
        var permission: String? = null

        abstract fun create(ride: TrackedRide, car: DynamicSeatedRideCar): Seat<out Entity>
    }
}