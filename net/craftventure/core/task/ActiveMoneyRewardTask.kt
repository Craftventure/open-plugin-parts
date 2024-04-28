package net.craftventure.core.task

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.CoinBoostManager
import net.craftventure.core.metadata.CvMetadata
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveCoinBooster
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveServerCoinBooster
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import org.bukkit.Bukkit
import org.bukkit.entity.Player


object ActiveMoneyRewardTask {
    private var isActive = false
    private var lastUpdateTime = System.currentTimeMillis()

    var activeCoinboosters: List<ActiveCoinBooster> = emptyList()
        private set
    var activeServerCoinboosters: List<ActiveServerCoinBooster> = emptyList()
        private set

    fun getActivePersonalBoosters(player: Player) = activeCoinboosters.filter { it.uuid == player.uniqueId }
    fun getActiveServerBoosters(player: Player) = activeServerCoinboosters.filter { it.activator == player.uniqueId }

    fun onActivated(booster: ActiveCoinBooster) {
        activeCoinboosters = activeCoinboosters.filter { it.id != booster.id } + booster
    }

    fun onActivated(booster: ActiveServerCoinBooster) {
        activeServerCoinboosters = activeServerCoinboosters.filter { it.id != booster.id } + booster
    }

    fun updateCachedBoostersSync() {
        activeCoinboosters = MainRepositoryProvider.activeCoinBoosterRepository.itemsPojo()
        activeServerCoinboosters = MainRepositoryProvider.activeServerCoinBoosterRepository.itemsPojo()
    }

    fun init() {
        if (isActive)
            return
        isActive = true

        lastUpdateTime = System.currentTimeMillis()

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            executeAsync {
                val difference = System.currentTimeMillis() - lastUpdateTime
                if (Bukkit.getOnlinePlayers().any { !it.isCrew() } && PluginProvider.isProductionServer()) {
                    MainRepositoryProvider.activeServerCoinBoosterRepository.update(difference)
                }
                lastUpdateTime = System.currentTimeMillis()

                MainRepositoryProvider.activeCoinBoosterRepository.cleanup()
                try {
                    //                            long start = System.currentTimeMillis();
                    for (player in ArrayList(Bukkit.getOnlinePlayers())) {
                        val cvMetadata = player.getMetadata<CvMetadata>()
                        if (cvMetadata != null && !cvMetadata.afkStatus.isAfk) {
                            val coins = CoinBoostManager.getCoinRewardPerMinute(player)
//                                if (player.isCrew()) {
//                                    val block = player.location.block
//                                    if (block.type == Material.WATER || block.type == Material.STATIONARY_WATER ||
//                                            block.type == Material.LAVA || block.type == Material.STATIONARY_LAVA) {
//                                        coins = 0
//                                    }
//                                }
                            //                                    Logger.console("Awarding " + player.getName() + " " + coins + "VC");
                            MainRepositoryProvider.bankAccountRepository.delta(
                                player.uniqueId,
                                BankAccountType.VC,
                                coins.toLong(),
                                TransactionType.ACTIVE_ONLINE_REWARD
                            )
                        }
                    }
                } catch (e: Exception) {
                    Logger.capture(e)
                }

                updateCachedBoostersSync()
            }
        }, (20 * 15).toLong(), (1 * 60 * 20).toLong())
    }
}
