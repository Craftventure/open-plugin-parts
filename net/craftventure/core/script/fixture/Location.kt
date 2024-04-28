package net.craftventure.core.script.fixture

import org.bukkit.util.Vector

data class Location(
    var x: Double,
    var y: Double,
    var z: Double
) {
    fun toVector() = Vector(x, y, z)
}