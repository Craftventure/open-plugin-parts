package net.craftventure.core.ride

import net.craftventure.bukkit.ktx.extension.renewPotionEffect
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


class PukeEffect private constructor(
    private val player: Player,
    private val offset: Int = CraftventureCore.getRandom().nextInt(20 * 2) + 20 * 4,
    private val rewardAchievement: Boolean = true,
    val duration: Int = (Math.random() * 20 * 3).toInt() + 20,
    isIstant: Boolean = false
) : Runnable {
    private var currentTick = 0

    init {
        player.sendMessage(Translation.RIDE_PUKE.getTranslation(player)!!)
        val taskId =
            Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), this, offset.toLong(), 1L)

        player.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, duration + offset, 1, true, false))
        Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
            Bukkit.getScheduler().cancelTask(taskId)
            player.renewPotionEffect(
                PotionEffectType.HUNGER,
                duration = 80 + CraftventureCore.getRandom().nextInt(80)
            )
            if (rewardAchievement) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.increaseCounter(player.uniqueId, "puked")
                }
            }
        }, if (isIstant) 0 else (offset + duration).toLong())
    }

    override fun run() {
        if (currentTick % 3 == 0) {
            player.eyeLocation.clone()
                .add(player.location.direction.normalize().multiply(0.4))
                .spawnParticleX(
                    Particle.SLIME,
                    100,
                    0.2, 0.2, 0.2
                )
        }
        if (currentTick % 10 == 0)
            player.world.playSound(player.location, Sound.ENTITY_PLAYER_BURP, 1.0f, Math.random().toFloat() * 2)

        currentTick++
    }

    companion object {
        fun play(
            player: Player,
            offset: Int = CraftventureCore.getRandom().nextInt(20 * 2) + 20 * 4,
            rewardAchievement: Boolean = true,
            duration: Int = (Math.random() * 20 * 3).toInt() + 20,
            isInstant: Boolean = false
        ) {
            PukeEffect(player, offset, rewardAchievement, duration, isInstant)
        }

        fun playInstant(player: Player) {
            PukeEffect(player, isIstant = true)
        }

        @JvmOverloads
        fun playRandom(player: Player, chance: Double, rewardAchievement: Boolean = true) {
            if (CraftventureCore.getRandom().nextDouble() <= chance || DateUtils.isAprilFools) {
                PukeEffect(player, rewardAchievement = rewardAchievement)
            }
        }
    }
}
