package net.craftventure.core.feature.minigame.lasergame.turret

import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Location
import org.bukkit.util.BoundingBox

abstract class BaseSingleTargetTurret(
    game: LaserGame,
    currentLocation: Location,
    boundingBoxes: Array<BoundingBox>,
    owner: LaserGameEntity?,
    hp: Int,
    spawnSounds: Collection<String>,
    spottedSounds: Collection<String>,
    spottedLostSounds: Collection<String>,
    moveSounds: Collection<String>,
    destroySounds: Collection<String>,
    shootSounds: Collection<String>
) : BaseTurret(
    game,
    currentLocation,
    boundingBoxes,
    owner,
    hp,
    spawnSounds,
    spottedSounds,
    spottedLostSounds,
    moveSounds,
    destroySounds,
    shootSounds
) {
    protected var lastTargetChange = System.currentTimeMillis()
    protected var currentTarget: LaserGameEntity? = null
        set(value) {
            if (field !== value) {
                field = value
                lastTargetChange = System.currentTimeMillis()
                Logger.debug("Target changed to ${value?.name} $value")
                if (value != null)
                    playSpottedSound()
                else
                    playSpottedLostSound()
            }
        }

    protected var movementSpeedInDegreesPerTick = 4.0
    protected var maxPitch = 45.0
    protected var minPitch = -35.0
    protected var maxReach = 15.0

    protected fun acquireTarget(): LaserGameEntity? {
        val currentTarget = this.currentTarget
        if (currentTarget != null && currentTarget.canBeHit()) {
            val distance = currentTarget.currentLocation.distance(currentLocation)
//            Logger.debug("Distance ${distance.format(2)}")
            if (distance < maxReach) {
                return currentTarget
            }
        }
//        Logger.debug("Picking new target, old=$currentTarget")
        val targets = game.targets
            .filter { (owner == null || it.isEnemy(this)) && it.currentLocation.distance(currentLocation) < maxReach && it.canBeHit() }
//        Logger.debug("Targets ${targets.size} ${targets.joinToString(", ") {
//            it.currentLocation.distance(currentLocation).toString()
//        }}")
        this.currentTarget = if (targets.isNotEmpty()) targets.random() else null
        return this.currentTarget
    }
}