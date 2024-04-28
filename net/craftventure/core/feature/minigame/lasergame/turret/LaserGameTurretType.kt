package net.craftventure.core.feature.minigame.lasergame.turret

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import org.bukkit.Location

enum class LaserGameTurretType(
    val displayName: String,
    val factory: (location: Location, game: LaserGame) -> LaserGameTurret
) {
    DEFAULT("Default", factory = { location, game -> DefaultTurret(location, game) })
}