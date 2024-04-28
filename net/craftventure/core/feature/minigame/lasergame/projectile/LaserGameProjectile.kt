package net.craftventure.core.feature.minigame.lasergame.projectile

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity

interface LaserGameProjectile {
    val shooter: LaserGameEntity
    val isValid: Boolean
    val damageType: LaserGameEntity.DamageType
    fun update(game: LaserGame)
}