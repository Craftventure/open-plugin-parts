package net.craftventure.core.feature.minigame.snowball

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.core.async.executeAsync
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigamePlayer
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.type.MinigameScoreType
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.EntityEffect
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*

class SnowballPlayer(
    val player: Player,
    val team: Team
) {
    var kills = 0
        private set
    var assists = 0
        private set
    var deaths = 0
        private set
    var hitsTakenThisLife = 0
        private set
    val maxHits = 3
    var lastHitAt: Long? = null
        private set
    var isDead = false
        private set

    init {
        player.absorptionAmount = (maxHits - hitsTakenThisLife).toFloat() * 2.0
    }

    fun hit(cause: MinigamePlayer<SnowballPlayer>) {
        if (isDead)
            return

        player.playEffect(EntityEffect.HURT)
        lastHitAt = System.currentTimeMillis()
        hitsTakenThisLife++

        if (hitsTakenThisLife >= maxHits) {
            cause.metadata.kills++
            deaths++
            isDead = true
//            player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 20 * 1000, maxHits - hitsTakenThisLife, true, false), true)
            player.absorptionAmount = (maxHits - hitsTakenThisLife).toFloat() * 2.0
        } else {
//            player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 20 * 1000, maxHits - hitsTakenThisLife, true, false), true)
            player.absorptionAmount = (maxHits - hitsTakenThisLife).toFloat() * 2.0
        }
    }

    fun revive() {
        isDead = false
        hitsTakenThisLife = 0
        lastHitAt = null
        player.absorptionAmount = (maxHits - hitsTakenThisLife).toFloat() * 2.0
    }

    fun saveScores(minigame: Minigame) {

        executeAsync {
            val database = MainRepositoryProvider.minigameScoreRepository
            val killScore = database.find(player.uniqueId, minigame.internalName, MinigameScoreType.KILLS)

            if (killScore != null) {
                database.deltaScore(
                    player.uniqueId,
                    minigame.internalName,
                    MinigameScoreType.KILLS,
                    kills.toLong()
                )
            } else {
                database.create(
                    MinigameScore(
                        UUID.randomUUID(),
                        player.uniqueId,
                        minigame.internalName,
                        kills,
                        LocalDateTime.now(),
                        MinigameScoreType.KILLS,
                        null,
                        player.isCrew()
                    )
                )
            }
            val deathScore = database.find(player.uniqueId, minigame.internalName, MinigameScoreType.DEATHS)

            if (deathScore != null) {
                database.deltaScore(
                    player.uniqueId,
                    minigame.internalName,
                    MinigameScoreType.DEATHS,
                    deaths.toLong()
                )
            } else {
                database.create(
                    MinigameScore(
                        UUID.randomUUID(),
                        player.uniqueId,
                        minigame.internalName,
                        deaths,
                        LocalDateTime.now(),
                        MinigameScoreType.DEATHS,
                        null,
                        player.isCrew()
                    )
                )
            }
        }
    }

    enum class Team(val color: TextColor) {
        RED(NamedTextColor.RED),
        BLUE(NamedTextColor.BLUE)
    }
}