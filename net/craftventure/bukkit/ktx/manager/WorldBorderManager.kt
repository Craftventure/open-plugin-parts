package net.craftventure.bukkit.ktx.manager

import net.craftventure.bukkit.ktx.area.Area

object WorldBorderManager {
    var borderParts: List<BorderPart> = emptyList()

    data class BorderPart(
        val crewOnly: Boolean = true,
        val renderTiles: Boolean = false,
        val area: Area
    )
}