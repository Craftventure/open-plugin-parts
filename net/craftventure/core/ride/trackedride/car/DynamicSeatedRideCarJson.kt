package net.craftventure.core.ride.trackedride.car

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.trackedride.RideCar
import net.craftventure.core.ride.trackedride.TrackedRide
import net.craftventure.core.ride.trackedride.car.seat.Seat

@JsonClass(generateAdapter = true)
class DynamicSeatedRideCarJson : RideCar.Json() {
    lateinit var seats: List<Seat.Json>

    override fun create(ride: TrackedRide): RideCar = DynamicSeatedRideCar(ride.name, length).also { car ->
        seats.forEach { seat ->
            car.addSeat(seat.create(ride, car))
        }
        car.restore(this)
    }
}