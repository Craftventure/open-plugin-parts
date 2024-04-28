package net.craftventure.database.bukkit.extensions

import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.permissions.Permissible


fun Warp.teleport(player: Player, async: Boolean = true) {
    if (async)
        player.teleportAsync(toLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN)
    else
        player.teleport(toLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN)
}

fun Warp.teleportIfPermissioned(
    player: Player,
    teleportCause: PlayerTeleportEvent.TeleportCause = PlayerTeleportEvent.TeleportCause.PLUGIN,
    force: Boolean = false,
): Boolean {
    if (force || permission == null || permission!!.isNotEmpty() && player.hasPermission(permission!!)) {
        player.teleportAsync(toLocation(), teleportCause)
        return true
    }
    return false
}

fun Warp.isAllowed(permissible: Permissible): Boolean {
    return permission == null || permissible.hasPermission(permission!!)
}

fun Warp.toLocation(): Location {
    return Location(Bukkit.getWorld(world!!), x!!, y!!, z!!, yaw!!.toFloat(), pitch!!.toFloat())
}

fun Warp.setLocation(location: Location) {
    x = location.x
    y = location.y
    z = location.z
    yaw = location.yaw.toDouble()
    pitch = location.pitch.toDouble()
}