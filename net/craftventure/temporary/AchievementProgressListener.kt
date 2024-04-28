package net.craftventure.temporary

import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.map.renderer.MapManager
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.Achievement
import net.craftventure.database.generated.cvdata.tables.pojos.AchievementProgress
import net.craftventure.database.repository.AchievementProgressRepository
import net.craftventure.database.repository.BaseIdRepository
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class AchievementProgressListener : BaseIdRepository.Listener<AchievementProgress>() {
    override fun onInsert(item: AchievementProgress) {
        handle(item)
    }

    override fun onUpdate(item: AchievementProgress) {
        handle(item)
    }

    override fun onMerge(item: AchievementProgress) {
        handle(item)
    }

    private fun handle(item: AchievementProgress) {
        val achievement = MainRepositoryProvider.achievementRepository.findCached(item.achievementId!!) ?: return
        executeSync {
            if ((item.count ?: 0) > 1) {
                onAchieventCounterIncreased(item.uuid!!, achievement, item)
            } else {
                onAchievementAwarded(item.uuid!!, achievement)
            }
        }
    }

    fun onAchieventCounterIncreased(
        uuid: UUID,
        achievement: Achievement,
        rewardedAchievement: AchievementProgress
    ) {
        MapManager.instance.invalidateForAchievement(achievement.id)
        if (AchievementProgressRepository.DEBUG)
            logcat { "Increased achievement ${achievement.id} for $uuid" }
//        val player = Bukkit.getPlayer(uuid) ?: return
        when (achievement.id) {
            "minigame_beerbrawl_tavern2_win" -> {
                executeAsync {
                    if (rewardedAchievement.count!! >= 50)
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(
                            uuid,
                            "title_blessed_brawler",
                            -1
                        )
                    if (rewardedAchievement.count!! >= 100)
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(
                            uuid,
                            "title_holy_brawler",
                            -1
                        )
                }
            }

            "minigame_beerbrawl_tavern2_beer_deliver" -> {
                executeAsync {
                    if (rewardedAchievement.count!! >= 500)
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(
                            uuid,
                            "title_holy_friend",
                            -1
                        )
                }
            }

            "summer_2019_fish_killer" -> {
                executeAsync {
                    if (rewardedAchievement.count!! >= 1001) {
                        val succes = MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(
                            uuid,
                            "title_summer_2019_fish_killer_master",
                            -1
                        )
                    } else if (rewardedAchievement.count!! >= 100) {
                        val succes = MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(
                            uuid,
                            "title_summer_2019_fish_killer",
                            -1
                        )
                    }
                }
            }

            "drink_alcohol" -> if (rewardedAchievement.count!! >= 1000) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "drink_alcohol_1000")
                    MainRepositoryProvider.playerOwnedItemRepository
                        .createOneLimited(uuid, "title_alcoholic", -1)
                }
            } else if (rewardedAchievement.count!! >= 100) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "drink_alcohol_100")
                }
            } else if (rewardedAchievement.count!! >= 5) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "drink_alcohol_5")
                }
            } else if (rewardedAchievement.count!! >= 1) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "drink_alcohol_1")
                }
            }

            "winter2018_present" -> if (rewardedAchievement.count!! >= 100) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "winter2018_presentoverload")
                }
            }

            "winter2018_dailychest" -> if (rewardedAchievement.count!! >= 15) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "winter2018_dailychestoverload")
                    val result = MainRepositoryProvider.playerOwnedItemRepository
                        .createOneLimited(uuid, "title_winter2018_dailychest", -1)
                }
            }

            "winter2020_dailychest" -> if (rewardedAchievement.count!! >= 15) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "winter2020_dailychestoverload")
                    val result = MainRepositoryProvider.playerOwnedItemRepository
                        .createOneLimited(uuid, "title_winter2020_dailychest", -1)
                }
            }

            "minigame_snowtopia2018_win" -> if (rewardedAchievement.count!! >= 10) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "minigame_snowtopia2018_win_10x")
                }
            }

            "minigame_roartopia2018_win" -> if (rewardedAchievement.count!! >= 10) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "minigame_roartopia2018_win_10x")
                }
            }

            "minigame_snowfight2018_win" -> if (rewardedAchievement.count!! >= 10) {
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository.reward(uuid, "minigame_snowfight2018_win_10x")
                }
            }

            "cookietorch_thrower" -> if (rewardedAchievement.count!! >= 500) {
                executeAsync {
                    val result = MainRepositoryProvider.playerOwnedItemRepository
                        .createOneLimited(uuid, "title_cookietorch", -1)
                }
            }

            "eat_napoleon" -> if (rewardedAchievement.count!! >= 1000) {
                executeAsync {
                    val result = MainRepositoryProvider.playerOwnedItemRepository
                        .createOneLimited(uuid, "title_napoleon", -1)
                }
            }
        }
        //        Player player = Bukkit.getPlayer(uuid);
        //        if (player != null) {
        //            if (achievement != null) {
        //                if (rewardedAchievement != null) {
        //                    player.sendMessage(Translation.ACHIEVEMENT_INCREASED.getTranslation(player, achievement.getDisplayName(), rewardedAchievement.getCount()));
        //                    SoundUtils.playToPlayer(player, player.getLocation(), SoundUtils.ACHIEVEMENT, 10f, 1f);
        //                }
        //            }
        //        }
    }

    fun onAchievementAwarded(uuid: UUID, achievement: Achievement) {
        MapManager.instance.invalidateForAchievement(achievement.id)
        if (AchievementProgressRepository.DEBUG)
            logcat { "Rewarded achievement ${achievement.id} to $uuid" }
        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            if (achievement.description != null)
                player.sendMessage(
                    Translation.ACHIEVEMENT_REWARD.getTranslation(
                        player,
                        achievement.displayName,
                        achievement.description,
                        achievement.vcWorth,
                        achievement.bankAccountType!!.abbreviation
                    )!!
                )
            else
                player.sendMessage(
                    Translation.ACHIEVEMENT_REWARD_WITHOUT_DESCRIPTION.getTranslation(
                        player,
                        achievement.displayName,
                        achievement.vcWorth,
                        achievement.bankAccountType!!.abbreviation
                    )!!
                )
            player.playSound(player.location, SoundUtils.ACHIEVEMENT, 10f, 1f)
            AchievementRewardedEffect(player)
                .runTaskTimer(CraftventureCore.getInstance(), 1L, 1L)
        }
    }

    class AchievementRewardedEffect(private val player: Player) : BukkitRunnable() {
        private var height = 0.0
        private var tick = 0
        private var angle = 0.0

        override fun run() {
            height += 0.08

            val location = player.location
            location.world?.spawnParticleX(
                Particle.VILLAGER_HAPPY,
                location.x + Math.cos(angle), location.y + height, location.z + Math.sin(angle)
            )
            location.world?.spawnParticleX(
                Particle.VILLAGER_HAPPY,
                location.x + Math.cos(-angle), location.y + (20.0 * 1.5 * 0.08 - height), location.z + Math.sin(-angle)
            )

            angle += 0.5
            tick++
            if (tick > 20 * 1.5) {
                cancel()
            }
        }
    }
}