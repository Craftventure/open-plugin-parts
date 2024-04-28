package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.plugin.PluginState
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.BankAccountType
import net.craftventure.temporary.createOneLimited
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor

class CvMetadata(
    val player: Player
) : BasePlayerMetadata(player) {
    val afkStatus = player.getOrCreateMetadata { AfkStatus(player) }

    private val joinTime = System.currentTimeMillis()
    private var joinOnlineTime = 0L
    private var joinAfkTime = 0L

    init {
        executeAsync {
            val guestStat = MainRepositoryProvider.guestStatRepository.find(player.uniqueId)
            val bankAccount = MainRepositoryProvider.bankAccountRepository.get(player.uniqueId, BankAccountType.VC)
            if (guestStat == null || bankAccount == null) {
                executeSync {
                    player.kick(
                        Component.text(
                            "There were some technical difficulties while joining. Please retry or contact the crew, preferrably at discord.Craftventure.net",
                            CVTextColor.serverError
                        )
                    )
                }
                return@executeAsync
            }

            joinOnlineTime = guestStat.totalOnlineTime!!.toLong()
            joinAfkTime = guestStat.totalAfkTime!!.toLong()
            val start = guestStat.firstSeen
            val now = LocalDateTime.now()
            val years = ChronoUnit.YEARS.between(start, now)

            val rewardedAchievementDatabase = MainRepositoryProvider.achievementProgressRepository
            val playerOwnedItemDatabase = MainRepositoryProvider.playerOwnedItemRepository

            for (year in 1..years) {
                val birthdayId = String.format("birthday_%s_year", year)
                rewardedAchievementDatabase.reward(player.uniqueId, birthdayId)
                reward(player, birthdayId)
                playerOwnedItemDatabase.createOneLimited(
                    player.uniqueId,
                    String.format("title_birthday_%s", year),
                    -1,
                    true
                )
            }
        }
    }

    val currentTotalOnlineTimeInMs get() = joinOnlineTime * 1000 + (System.currentTimeMillis() - joinTime)
    val currentActiveOnlineTimeInMs get() = joinOnlineTime * 1000 + (System.currentTimeMillis() - joinTime) - afkStatus.totalAfkMillisThisSession - joinAfkTime * 1000

    override fun onDestroy() {
        super.onDestroy()
        stop(CraftventureCore.getState() == PluginState.ENABLED)
    }

    private fun stop(async: Boolean) {
        afkStatus.stop()

        val playerUuid = player.uniqueId
        val seconds = floor(((System.currentTimeMillis() - joinTime) / 1000f).toDouble()).toInt()
        val afkSeconds = floor((afkStatus.totalAfkMillisThisSession / 1000f).toDouble()).toInt()
        if (!async) {
            MainRepositoryProvider.guestStatRepository.deltaOnlineAndAfkTimes(
                playerUuid,
                seconds.toLong(),
                afkSeconds.toLong()
            )
        } else {
            executeAsync {
                MainRepositoryProvider.guestStatRepository.deltaOnlineAndAfkTimes(
                    playerUuid,
                    seconds.toLong(),
                    afkSeconds.toLong()
                )
            }
        }
    }

    override fun debugComponent() =
        Component.text("joinTime=$joinTime joinOnlineTime=$joinOnlineTime joinAfkTime=$joinAfkTime currentTotalOnlineTimeInMs=$currentTotalOnlineTimeInMs currentActiveOnlineTimeInMs=$currentActiveOnlineTimeInMs")

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { CvMetadata(player) }
    }
}