package net.craftventure.core.feature.minigame.lasergame.turret

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.feature.minigame.lasergame.LaserGamePlayer
import org.bukkit.Location

interface LaserGameTurret : LaserGameEntity {
    val isValid: Boolean
    fun update(game: LaserGame)
    fun canBeReclaimed(): Boolean
    fun interactWith(player: LaserGamePlayer, location: Location): Boolean
    fun interactWith(player: LaserGamePlayer, entityId: Int): Boolean
    fun spawn()
    fun destroy(silent: Boolean)
}