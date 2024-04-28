package net.craftventure.database.bukkit.extensions

import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import net.craftventure.database.repository.WarpRepository
import org.bukkit.Location
import org.bukkit.permissions.Permissible
import org.bukkit.util.Vector


fun WarpRepository.getNearestWarpFromLocation(targetLocation: Location, permissible: Permissible?): Warp? {
    val locationVector = targetLocation.toVector()
    var foundWarp: Warp? = null
    var distance = 1000000.0
    //        Logger.console("Checking nearest warp for " + locationVector.getX() + ", " + locationVector.getY() + ", " + locationVector.getZ());
    for (warp in cachedItems) {
        if (permissible == null || warp.isAllowed(permissible)) {
            //                Logger.console("  Checking warp " + warp.getName());
            try {
                val warpLocationVector = Vector(warp.x!!, warp.y!!, warp.z!!)
                val warpDistance = warpLocationVector.distanceSquared(locationVector)
                if (foundWarp == null || warpDistance < distance) {
                    distance = warpDistance
                    //                        Logger.console("Warp " + warp.getName() + " is now the closest");
                    foundWarp = warp
                }
            } catch (e: Exception) {
                // e.printStackTrace();
            }

        }
    }
    return foundWarp
}