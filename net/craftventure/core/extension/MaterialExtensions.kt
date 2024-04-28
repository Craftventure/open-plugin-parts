package net.craftventure.core.extension

import org.bukkit.Material
import org.bukkit.block.data.AnaloguePowerable
import org.bukkit.block.data.Powerable
import org.bukkit.block.data.type.*
import java.util.*

fun Material.getPermissionName(): String {
    return when {
        data.isAssignableFrom(Powerable::class.java) && name.contains("PLATE") -> "pressure_plate"
        data.isAssignableFrom(AnaloguePowerable::class.java) && name.contains("PLATE") -> "pressure_plate_analogue"
        data.isAssignableFrom(Door::class.java) -> "door"
        data.isAssignableFrom(Switch::class.java) -> "switch"
        data.isAssignableFrom(Gate::class.java) -> "gate"
        else -> this.name.lowercase(Locale.getDefault())
    }
}

fun Material.isSign() =
    this.data == Sign::class.java || this.data == WallSign::class.java// this.data.isAssignableFrom(Sign::class.java) || this.data.isAssignableFrom(WallSign::class.java)