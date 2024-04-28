package net.craftventure.core.feature.minigame.lasergame

import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.core.CraftventureCore
import org.bukkit.EntityEffect
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.max


class LaserGamePlayer(
    val game: LaserGame,
    val player: Player,
    var itemA: EquippedLaserGameItem,
    var itemB: EquippedLaserGameItem,
    var team: LaserGameTeam,
    damage: Int = 0,
    var life: Int = 3
) : LaserGameEntity {
    var damage = damage
        set(value) {
            field = value
            updateAbsorption()
        }
    var invincibleUntil: Long = 0
    var deaths: Int = 0
        private set
    override var kills: Int = 0
    var killStreak: Int = 0
        private set
    override val currentLocation: Location
        get() = player.location
    override val mainHitTarget: Location
        get() = currentLocation.clone().add(0.0, player.height * 0.5, 0.0)
    override val invincible: Boolean
        get() = System.currentTimeMillis() <= invincibleUntil
    override val isDead: Boolean
        get() = player.isDead
    override val name: String
        get() = player.name
    override var owner: LaserGameEntity? = null

    override fun isPartOfTarget(entity: Entity): Boolean = entity.entityId == player.entityId

    init {
        updateAbsorption()
    }

    override fun applyVelocity(velocity: Vector) {
        val playerVelocity = player.velocity
        playerVelocity.add(velocity)
//        Logger.debug("Changing player velocity of ${player.name} from ${player.velocity.asString()} to ${playerVelocity.asString()} (added ${velocity.asString()}")
        player.velocity = playerVelocity
    }

    private fun updateAbsorption() {
        player.absorptionAmount = if (isDead) 0.0 else max(0.0, (life - damage) * 2.0)
    }

    override fun awardKill() {
        kills++
        killStreak++
    }

    fun kill() {
        if (isDead) return
        player.playSound(
            player.location,
            "${SoundUtils.SOUND_PREFIX}:minigame.laser.dead${CraftventureCore.getRandom().nextInt(2) + 1}",
            1f,
            1f
        )
        player.health = 0.0
        deaths++
        killStreak = 0
        damage = 0
    }

    fun clearInvincibility() {
        invincibleUntil = 0
    }

    fun resetAfterRespawn() {
        killStreak = 0
        damage = 0
        invincibleUntil = System.currentTimeMillis() + 4000
        player.playSound(player.location, "${SoundUtils.SOUND_PREFIX}:minigame.laser.respawn", 1f, 1f)
//        player.playEffect(EntityEffect.TOTEM_RESURRECT)
        updateAbsorption()
    }

    fun cleanup() {
        player.absorptionAmount = 0.0
        player.level = 0
        player.exp = 0f
    }

    override fun canBeHit(): Boolean = !invincible && !isDead

    override fun hitBy(source: LaserGameEntity, damage: Int): LaserGameEntity.HitResult {
        if (!canBeHit()) return LaserGameEntity.HitResult.IGNORED

        this.damage += damage
        this.player.playEffect(if (damage > 1) EntityEffect.HURT_EXPLOSION else EntityEffect.HURT)
        if (this.damage >= this.life) {
            kill()
            return LaserGameEntity.HitResult.DESTROYED
        }

        return LaserGameEntity.HitResult.DAMAGED
    }

    override fun isHit(location: Location): LaserGameEntity.HitType? {
        val headLocation = this.player.eyeLocation
//        debugRadius(headLocation, HIT_RADIUS_HEAD, Color.RED)
        val headDistance = headLocation.distance(location)
//        Logger.debug("headDistance: ${headDistance.format(2)}")
        if (headDistance < HIT_RADIUS_HEAD) return LaserGameEntity.HitType.HEADSHOT

        val bodyLocation = this.player.location.add(0.0, player.height * 0.5, 0.0)
//        debugRadius(bodyLocation, HIT_RADIUS_BODY, Color.GREEN)
        val hit1 = bodyLocation.distance(location)
//        Logger.debug("hit1: ${hit1.format(2)}")
        if (hit1 < HIT_RADIUS_BODY) return LaserGameEntity.HitType.NORMAL

        val legsLocation = this.player.location.add(0.0, player.height * 0.37 * 0.5, 0.0)
//        debugRadius(legsLocation, HIT_RADIUS_LEGS, Color.BLUE)
        val hit2 = legsLocation.distance(location)
//        Logger.debug("hit2: ${hit2.format(2)}")
        if (hit2 < HIT_RADIUS_LEGS) return LaserGameEntity.HitType.NORMAL

        if (hit1 < HIT_RADIUS_NEAR || hit2 < HIT_RADIUS_NEAR || headDistance < HIT_RADIUS_NEAR) return LaserGameEntity.HitType.NEAR_HIT
        return null
    }

    val HIT_RADIUS_HEAD = 0.35 //0.3
    val HIT_RADIUS_BODY = 0.8 //0.5
    val HIT_RADIUS_LEGS = 00.4 //.25
    val HIT_RADIUS_NEAR = 1.8 //1.8
}