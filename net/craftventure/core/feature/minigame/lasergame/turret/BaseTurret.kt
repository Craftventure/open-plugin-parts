package net.craftventure.core.feature.minigame.lasergame.turret

import net.craftventure.bukkit.ktx.extension.playSound
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.minigame.lasergame.LaserGame
import net.craftventure.core.feature.minigame.lasergame.LaserGameEntity
import net.craftventure.core.feature.minigame.lasergame.LaserGamePlayer
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.utils.InterpolationUtils
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.SoundCategory
import org.bukkit.util.BoundingBox

abstract class BaseTurret(
    val game: LaserGame,
    override val currentLocation: Location,
    protected val boundingBoxes: Array<BoundingBox>,
    override var owner: LaserGameEntity?,
    hp: Int,
    val spawnSounds: Collection<String>,
    val spottedSounds: Collection<String>,
    val spottedLostSounds: Collection<String>,
    val moveSounds: Collection<String>,
    val destroySounds: Collection<String>,
    val shootSounds: Collection<String>
) : LaserGameTurret {
    var hp = hp
        private set(value) {
            if (field != value) {
                field = value
                Logger.debug("HP $value")
                onHpChanged()
            }
        }
    protected val spawnYaw = currentLocation.yaw.toDouble()
    protected var lastStateChangeTime = System.currentTimeMillis()
        private set
    protected val millisInCurrentState: Long
        get() = System.currentTimeMillis() - lastStateChangeTime
    protected var state = State.INITIALISING
        set(value) {
            if (value != field) {
                field = value
                lastStateChangeTime = System.currentTimeMillis()
                onStateChanged(value)
            }
        }
    override val isValid: Boolean
        get() = state != State.DESTROYED && hp > 0

    override val invincible: Boolean
        get() = false
    override val isDead: Boolean
        get() = state == State.DESTROYED
    override val name: String = nextTurretName()
    override val displayName: String = "Turret"

    val isDestroyed: Boolean
        get() = state == State.DESTROYED || isDead || !isValid

    override fun canBeReclaimed(): Boolean = state == State.SCANNING
    override fun canBeHit(): Boolean = state != State.DESTROYED

    protected open fun onHpChanged() {}
    protected abstract fun onStateChanged(newState: State)

    override fun hitBy(source: LaserGameEntity, damage: Int): LaserGameEntity.HitResult {
        if (!canBeHit()) return LaserGameEntity.HitResult.IGNORED

        if (state != State.DESTROYED) {
            hp -= damage
            if (hp <= 0) {
                state = State.DESTROYED
                return LaserGameEntity.HitResult.DESTROYED
            }
            return LaserGameEntity.HitResult.DAMAGED
        }
        return LaserGameEntity.HitResult.IGNORED
    }

    override fun isHit(location: Location): LaserGameEntity.HitType? {
        if (boundingBoxes.any { it.contains(location.x, location.y, location.z) }) {
            return LaserGameEntity.HitType.NORMAL
        }
        return null
    }

    override fun spawn() {
        if (state != State.INITIALISING) return
        state = State.BUILDING
        playSpawnSound()
    }

    override fun destroy(silent: Boolean) {
        state = State.DESTROYED
        if (!silent) {
            playDestroySound()

            currentLocation.clone().add(0.0, boundingBoxes.maxByOrNull { it.height }!!.height * 0.5, 0.0)
                .spawnParticleX(Particle.EXPLOSION_NORMAL, offsetX = 0.5, offsetY = 0.5, offsetZ = 0.5, count = 3)
        }
    }

    protected fun playShootSound() {
        if (shootSounds.isEmpty()) return
        val pitch = InterpolationUtils.linearInterpolate(0.8, 1.2, CraftventureCore.getRandom().nextDouble()).toFloat()
        currentLocation.playSound(shootSounds.random(), SoundCategory.AMBIENT, 1f, pitch)
    }

    protected fun playMoveSound() {
        if (moveSounds.isNotEmpty())
            currentLocation.playSound(moveSounds.random(), SoundCategory.AMBIENT, 1f, 1f)
    }

    protected fun playSpottedSound() {
        if (spottedSounds.isNotEmpty())
            currentLocation.playSound(spottedSounds.random(), SoundCategory.AMBIENT, 1f, 1f)
    }

    protected fun playSpottedLostSound() {
        if (spottedLostSounds.isNotEmpty())
            currentLocation.playSound(spottedLostSounds.random(), SoundCategory.AMBIENT, 1f, 1f)
    }

    protected fun playSpawnSound() {
        if (spawnSounds.isNotEmpty())
            currentLocation.playSound(spawnSounds.random(), SoundCategory.AMBIENT, 1f, 1f)
    }

    protected fun playDestroySound() {
        if (destroySounds.isNotEmpty())
            currentLocation.playSound(destroySounds.random(), SoundCategory.AMBIENT, 1f, 1f)
    }

    override fun interactWith(player: LaserGamePlayer, location: Location): Boolean {
        return false
    }

    override fun interactWith(player: LaserGamePlayer, entityId: Int): Boolean {
        return false
    }

    enum class State {
        INITIALISING,
        BUILDING,
        UPGRADING,
        SCANNING,
        DESTROYED
    }

    companion object {
        private var nextTurretId = 0
        fun nextTurretName() = "Turret" + (nextTurretId++)

        val defaultSpawnSounds: Collection<String> = listOf("${SoundUtils.SOUND_PREFIX}:minigame.laser.turret.built")
        val defaultSpottedSounds: Collection<String> = listOf("${SoundUtils.SOUND_PREFIX}:minigame.laser.turret.spot")
        val defaultSpottedLostSounds: Collection<String> = listOf()
        val defaultMoveSounds: Collection<String> = listOf("${SoundUtils.SOUND_PREFIX}:minigame.laser.turret.scan")
        val defaultDestroySounds: Collection<String> =
            listOf("${SoundUtils.SOUND_PREFIX}:minigame.laser.turret.destroy")
        val defaultShootSounds: Collection<String> =
            (1..5).map { "${SoundUtils.SOUND_PREFIX}:minigame.laser.laser.$it" }
    }
}