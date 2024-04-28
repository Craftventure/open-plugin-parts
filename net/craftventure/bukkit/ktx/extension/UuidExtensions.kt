package net.craftventure.bukkit.ktx.extension

import org.bukkit.Bukkit
import java.util.*

val UUID.player
    get() = Bukkit.getPlayer(this)

fun UUID.withV2Marker() = toString().let { uuidString ->
    UUID.fromString(uuidString.substring(0, 14) + "2" + uuidString.substring(15))
}