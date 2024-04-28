package net.craftventure.core.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.craftventure.core.database.VirtualItemsProvider
import net.craftventure.database.ConfigurationRepositoryProvider
import net.craftventure.database.DatabaseLoggerListener
import net.craftventure.database.MainRepositoryHolder
import net.craftventure.database.bukkit.listener.ItemStackDataCacheListener
import net.craftventure.database.bukkit.listener.RideListener
import net.craftventure.database.bukkit.listener.ShopCacheListener
import net.craftventure.database.repository.BaseIdRepository
import net.craftventure.temporary.*
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration

object Repository {
    private val config = HikariConfig().apply { }

    private val dataSource = HikariDataSource(config)
    private val configuration = DefaultConfiguration()
        .set(dataSource)
        .set(SQLDialect.MARIADB)
        .set(DatabaseLoggerListener())

    //        .set(DatabaseLoggerListener())
    private val provider = ConfigurationRepositoryProvider(configuration)

    init {
        MainRepositoryHolder.provider = provider
        // Listeners before caching
        provider.itemStackDataRepository.addListener(ItemStackDataCacheListener)
        provider.shopRepository.addListener(ShopCacheListener)
        provider.playerEquippedItemRepository.addListener(PlayerEquippedItemListener())
        provider.playerKeyValueRepository.addListener(PlayerKeyValueMapListener())
        provider.activeServerCoinBoosterRepository.addListener(ActiveServerCoinBoosterActivationListener())
        provider.minigameScoreRepository.addListener(MinigameScoreListener())
        provider.bankAccountRepository.addListener(BankAccountListener())
        provider.rideCounterRepository.addListener(RideCounterListener())
        provider.achievementProgressRepository.addListener(AchievementProgressListener())
        provider.activeCoinBoosterRepository.addListener(ActiveCoinBoosterActivationListener())
        provider.teamScoreRepository.addListener(TeamScoreListener())

        // Build cache
        provider.allRepositories.filterIsInstance(BaseIdRepository::class.java).forEach { repository ->
            if (repository.shouldCache)
                repository.requireCache()
        }

        // Listeners after caching
        provider.rideRepository.addListener(RideListener())
        provider.playerOwnedItemRepository.addListener(PlayerOwnedItemListener())
        provider.playerOwnedItemRepository.addVirtualItemProvider(VirtualItemsProvider)
    }

    fun init() {}

    fun destroy() {
        dataSource.close()
    }
}