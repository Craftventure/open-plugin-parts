package net.craftventure.database.bukkit.util

import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.database.bukkit.extensions.toLocation
import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import net.craftventure.database.generated.cvdata.tables.pojos.RideLink
import net.craftventure.database.type.RideState
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.material.Openable

object RideLinkHelper {
    fun isValidType(block: Block?): Boolean {
        if (block == null)
            return false
        return block.state is Sign || block.state.data is Openable
    }

    @JvmOverloads
    fun updateLink(ride: Ride, rideLink: RideLink, rideState: RideState = ride.state!!) {
        val location = rideLink.toLocation()
        val block = location.block
        if (block.state.data is Openable) {
            block.open(rideState.isOpen)
        }
    }
}