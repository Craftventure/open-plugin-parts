package net.craftventure.core.ride.trackedride.car.seat

import com.squareup.moshi.JsonClass
import net.craftventure.core.metadata.TypedInstanceOwnerMetadata.Companion.setOwner
import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.TrackedRide
import net.craftventure.core.ride.trackedride.TrackedRideHelper
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Minecart


class MinecartSeat(
    rightOffset: Double,
    upOffset: Double,
    forwardOffset: Double,
    passengerCar: Boolean,
    entityName: String
) : Seat<Minecart>(
    rightOffset,
    upOffset,
    forwardOffset,
    passengerCar,
    entityName,
    90f
) {
    private val pitchFix = Math.toRadians(90.0)

    init {
        this.setShouldAlwaysTeleport(true)
    }

    override fun spawn(
        world: World,
        x: Double,
        y: Double,
        z: Double,
        yaw: Double,
        pitch: Double,
        car: RideCar
    ): Minecart {
        val minecart = world.spawn(Location(world, x, y, z, yaw.toFloat(), pitch.toFloat()), Minecart::class.java)
        TrackedRideHelper.setCarEntity(minecart, car)
        minecart.setOwner(car.trackedRide!!)
        minecart.setGravity(false)
        minecart.maxSpeed = 0.0
        minecart.isSilent = true
        return minecart
    }

    override fun move(
        x: Double,
        y: Double,
        z: Double,
        trackYawRadian: Double,
        trackPitchRadian: Double,
        bankingDegree: Double,
        forceUpdate: Boolean,
        car: RideCar
    ) {
        super.move(x, y, z, trackYawRadian, trackPitchRadian + pitchFix, bankingDegree, forceUpdate, car)
    }

    override fun move(
        entity: Minecart,
        x: Double,
        y: Double,
        z: Double,
        trackYawRadian: Double,
        trackPitchRadian: Double,
        bankingDegree: Double,
        forceUpdate: Boolean,
        car: RideCar
    ) {
        //        if (model != null) {
        //            entity.setHeadPose(new EulerAngle(-(trackPitchRadian + (Math.PI * 0.5)), 0, Math.toRadians(bankingDegree)));
        //        }
    }

    @JsonClass(generateAdapter = true)
    class Json : Seat.Json() {
        override fun create(ride: TrackedRide, car: DynamicSeatedRideCar) = MinecartSeat(
            rightOffset,
            upOffset,
            forwardOffset,
            isPassengerCar,
            ride.name,
        )
    }
}
