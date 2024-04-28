package net.craftventure.core.ride

import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.database.generated.cvdata.tables.pojos.Ride

interface RideInstance {
    val id: String
    val ride: Ride?

    fun displayName() = ride?.displayName ?: id

    //    val queue: RideQueue?
    fun getQueues(): Set<RideQueue>
}