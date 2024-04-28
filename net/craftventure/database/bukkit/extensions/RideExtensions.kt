package net.craftventure.database.bukkit.extensions

import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import java.time.LocalDateTime

val Ride.isNew: Boolean
    get() = openSince?.isAfter(LocalDateTime.now().minusDays(30)) ?: false