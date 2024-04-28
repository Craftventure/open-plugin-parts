package net.craftventure.core.config

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.SimpleArea

@JsonClass(generateAdapter = true)
data class BorderConfig(
    val xMin: Long = 0,
    val xMax: Long = 0,
    val zMin: Long = 0,
    val zMax: Long = 0,
    val crewOnly: Boolean = true,
    val renderTiles: Boolean = false,
) {
    val area: Area by lazy {
        SimpleArea("world", xMin.toDouble(), -100.0, zMin.toDouble(), xMax.toDouble(), 300.0, zMax.toDouble())
    }
}