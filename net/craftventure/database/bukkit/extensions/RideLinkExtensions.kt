package net.craftventure.database.bukkit.extensions

import net.craftventure.database.generated.cvdata.tables.pojos.RideLink
import org.bukkit.Bukkit
import org.bukkit.Location

fun RideLink.toLocation() = Location(
    Bukkit.getWorld(world!!),
    x!!.toDouble(),
    y!!.toDouble(),
    z!!.toDouble(),
)
