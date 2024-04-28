package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.isPayedVIP
import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreBuilder
import net.craftventure.chat.bungee.util.CVChatColor
import net.craftventure.core.config.AreaConfig
import net.craftventure.core.config.AreaConfigManager
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.extension.utcMillis
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.metadata.GenericPlayerMeta
import net.craftventure.core.metadata.OwnedItemCache
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveCoinBooster
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveServerCoinBooster
import net.craftventure.database.generated.cvdata.tables.pojos.CoinBooster
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.CoinBoosterType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.time.LocalDateTime


fun List<CoinBoostManager.CoinBoost>.rewardPerMinute(): Int {
    var coins = 0

    for (boost in this) {
        if (boost.type == CoinBoosterType.ADD)
            coins += boost.value
    }
    for (boost in this) {
        if (boost.type == CoinBoosterType.MULTIPLY)
            coins *= boost.value
    }

    return coins
}

object CoinBoostManager {
    fun getCoinRewardPerMinute(player: Player): Int {
        return getAllBoosts(player).rewardPerMinute()
    }

    fun getAllBoosts(player: Player): List<CoinBoost> {
        val time = LocalDateTime.now()
        val addBoosts = mutableListOf<CoinBoost>()
        val multiplyBoosts = mutableListOf<CoinBoost>()
        addBoosts.add(
            CoinBoost(
                1, CoinBoosterType.ADD, SourceType.BASE, "Online",
                sourceDescription = "You receive this boost while being online while not being AFK"
            )
        )

        if (player.isPayedVIP())
            addBoosts.add(
                CoinBoost(
                    1, CoinBoosterType.ADD, SourceType.RANK, "VIP",
                    sourceDescription = "You receive this boost for being an awesome VIP. Thanks for supporting our server and community!"
                )
            )
        else if (player.isVIP())
            addBoosts.add(
                CoinBoost(
                    1, CoinBoosterType.ADD, SourceType.RANK, "VIP (trial)",
                    sourceDescription = "You receive this boost for being VIP!"
                )
            )

        if (player.getMetadata<GenericPlayerMeta>()?.isNitroBoosting == true)
            addBoosts.add(
                CoinBoost(
                    1, CoinBoosterType.ADD, SourceType.EXTERNAL_REWARDS, "Nitro Booster",
                    sourceDescription = "You receive this boost as a thanks for boosting our Discord server. This booster may be changed and/or removed at any time"
                )
            )

        if (player.isCrew())
            addBoosts.add(
                CoinBoost(
                    1, CoinBoosterType.ADD, SourceType.RANK, "Crew",
                    sourceDescription = "You're a crewmember, do your job and get paid"
                )
            )

        if (player.getMetadata<OwnedItemCache>()?.ownedItemIds?.contains("hat_addict_5") == true) {
            addBoosts.add(
                CoinBoost(
                    1, CoinBoosterType.ADD, SourceType.CLOTHING, "Clothing",
                    sourceDescription = "Hat addict 5 bonus"
                )
            )
        }

//        if (player.isCrew()) {
//            val guestStat = CvMetadata.getOnly(player)?.currentActiveOnlineTimeInMs
//            addBoosts.add(CoinBoost(2, CoinBoosterType.ADD, SourceType.ONLINE_TIME_BONUS, "Online Time Bonus [Crew test]",
//                    sourceDescription = "For every 5 days you've been online & active at Craftventure you receive +1VC extra!"))
//        }

        for (activeServerCoinBooster in MainRepositoryProvider.activeServerCoinBoosterRepository.itemsPojo()
            .sortedBy { it.activated }) {
            val id = activeServerCoinBooster.boosterId
            if (id != null) {
                val booster = MainRepositoryProvider.coinBoosterRepository.findCached(id)
                if (booster != null) {
                    val value = booster.value!!.toInt()

                    if (booster.coinBoosterType == CoinBoosterType.ADD)
                        addBoosts.add(
                            CoinBoost(
                                value, booster.coinBoosterType!!, SourceType.SERVER_WIDE_BOOSTER,
                                "Serverwide coinbooster by ${activeServerCoinBooster.activator?.toName()}",
                                activeServerCoinBooster = activeServerCoinBooster
                            )
                        )
                    else if (booster.coinBoosterType == CoinBoosterType.MULTIPLY)
                        multiplyBoosts.add(
                            CoinBoost(
                                value, booster.coinBoosterType!!, SourceType.SERVER_WIDE_BOOSTER,
                                "Serverwide coinbooster by ${activeServerCoinBooster.activator?.toName()}",
                                activeServerCoinBooster = activeServerCoinBooster
                            )
                        )
                }
            }
        }

        var addArea: AreaConfig? = null
        var multiplyArea: AreaConfig? = null
        val areaConfigList = AreaConfigManager.getInstance().getAreaConfigList()
        for (i in areaConfigList.indices) {
            val areaConfig = areaConfigList[i]
            if (areaConfig.isActive && areaConfig.area.isInArea(player)) {
                if (areaConfig.coinAdd > 0 && (addArea == null || areaConfig.coinAdd > addArea.coinAdd))
                    addArea = areaConfig
                if (areaConfig.coinMultiply > 0 && (multiplyArea == null || areaConfig.coinMultiply > multiplyArea.coinMultiply))
                    multiplyArea = areaConfig
            }
        }

        if (addArea != null)
            addBoosts.add(
                CoinBoost(
                    addArea.coinAdd, CoinBoosterType.ADD, SourceType.AREA, addArea.displayName
                        ?: "Area",
                    sourceDescription = addArea.description
                        ?: "You receive this boost while being in the current area"
                )
            )
        if (multiplyArea != null)
            multiplyBoosts.add(
                CoinBoost(
                    multiplyArea.coinMultiply, CoinBoosterType.MULTIPLY, SourceType.AREA, multiplyArea.displayName
                        ?: "Area",
                    sourceDescription = multiplyArea.description
                        ?: "You receive this boost while being in the current area"
                )
            )

        try {
            val activeCoinBoosters = MainRepositoryProvider.activeCoinBoosterRepository.getByPlayer(player.uniqueId)
                .sortedBy { it.activated }
            for (i in activeCoinBoosters.indices) {
                val activeCoinBooster = activeCoinBoosters[i]
                if (activeCoinBooster.until!! > time) {
                    val coinBooster =
                        MainRepositoryProvider.coinBoosterRepository.findCached(activeCoinBooster.boosterId!!)
                    if (coinBooster != null) {
                        if (coinBooster.coinBoosterType == CoinBoosterType.ADD) {
                            addBoosts.add(
                                CoinBoost(
                                    coinBooster.value!!.toInt(), CoinBoosterType.ADD, SourceType.BOOSTER, "Coinbooster",
                                    activeCoinBooster = activeCoinBooster,
                                    coinBooster = coinBooster
                                )
                            )
                        } else if (coinBooster.coinBoosterType == CoinBoosterType.MULTIPLY) {
                            addBoosts.add(
                                CoinBoost(
                                    coinBooster.value!!.toInt(),
                                    CoinBoosterType.MULTIPLY,
                                    SourceType.BOOSTER,
                                    "Coinbooster",
                                    activeCoinBooster = activeCoinBooster,
                                    coinBooster = coinBooster
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }

        addBoosts.addAll(multiplyBoosts)
        return addBoosts
    }

    data class CoinBoost(
        val value: Int,
        val type: CoinBoosterType,
        val sourceType: SourceType,
        val sourceName: String,
        val sourceDescription: String? = null,
        val coinBooster: CoinBooster? = null,
        val activeCoinBooster: ActiveCoinBooster? = null,
        val activeServerCoinBooster: ActiveServerCoinBooster? = null
    ) {
        fun displayName(): String {
            if (activeServerCoinBooster != null) {
                return "Active Serverwide Coin Booster"
            } else if (coinBooster != null) {
                return "Activated Coin Booster"
            }
            return sourceName
        }

        fun displayDescription(): List<Component> {
            if (activeServerCoinBooster != null) {
                val uuid = activeServerCoinBooster.activator

                val loreBuilder = ComponentBuilder.LoreBuilder()
                val boosterId = activeServerCoinBooster.boosterId
                if (boosterId != null) {
                    val booster =
                        MainRepositoryProvider.coinBoosterRepository.cachedItems.firstOrNull { it.id == boosterId }
                    if (booster?.value != null) {
                        val value = booster.value
                        loreBuilder.text(
                            "Type "
                        )
                        loreBuilder.accented(
                            if (booster.coinBoosterType == CoinBoosterType.MULTIPLY)
                                "x$value"
                            else
                                "+$value"
                        )
                        loreBuilder.text(BankAccountType.VC.emoji, color = NamedTextColor.WHITE)
                        loreBuilder.emptyLines(1)
                    }
                }

                loreBuilder.text("Activated by ")
                loreBuilder.accented(uuid?.toName() ?: "?")
                loreBuilder.emptyLines(1)

                val timeLeft = activeServerCoinBooster.timeLeft ?: 0
                loreBuilder.text("Expires in around")
                loreBuilder.accented(DateUtils.formatWithoutSeconds(timeLeft.toLong(), "expired"))

                return loreBuilder.buildLineComponents()
            } else if (coinBooster != null && activeCoinBooster != null) {
                return loreBuilder {
                    text("Type ")
                    accented(
                        if (coinBooster.coinBoosterType == CoinBoosterType.MULTIPLY)
                            "x${coinBooster.value}"
                        else
                            "+${coinBooster.value}"
                    )
                    text(BankAccountType.VC.emoji, color = NamedTextColor.WHITE)
                    emptyLines(1)
                    text("Expires in ")
                    accented(
                        DateUtils.format(
                            activeCoinBooster.until!!.utcMillis - LocalDateTime.now().utcMillis,
                            "expired"
                        )
                    )
                }
            } else {
                val loreBuilder = ComponentBuilder.LoreBuilder()
                var lore =
                    CVChatColor.MENU_DEFAULT_LORE.toString() + "Type " + CVChatColor.MENU_DEFAULT_LORE_ACCENT + if (type == CoinBoosterType.MULTIPLY)
                        "x" + value + "VC"
                    else
                        "+" + value + "VC"
                if (sourceDescription != null) {
                    lore += "\n\n" + CVChatColor.MENU_DEFAULT_LORE + sourceDescription
                }
                return loreBuilder.buildLineComponents()
            }
        }
    }

    enum class SourceType {
        AREA,
        SERVER_WIDE_BOOSTER,
        BOOSTER,
        RANK,
        EXTERNAL_REWARDS,
        ONLINE_TIME_BONUS,
        CLOTHING,
        BASE
    }
}