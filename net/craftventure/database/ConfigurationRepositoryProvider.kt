package net.craftventure.database

import net.craftventure.database.repository.*
import org.jooq.Configuration
import org.jooq.SQLDialect

class ConfigurationRepositoryProvider(
    val configuration: Configuration
) : RepositoryProvider {
    override val achievementCategoryRepository = AchievementCategoryRepository(configuration)
    override val achievementRepository = AchievementRepository(configuration)
    override val casinoLogRepository = CasinoLogRepository(configuration)
    override val coinBoosterRepository = CoinBoosterRepository(configuration)
    override val configRepository = ConfigRepository(configuration)
    override val cachedGameProfileRepository = CachedGameProfileRepository(configuration)
    override val itemStackDataRepository = ItemStackDataRepository(configuration)
    override val mapEntriesRepository = MapEntriesRepository(configuration)
    override val ownableItemRepository = OwnableItemRepository(configuration)
    override val protectionRepository = ProtectionRepository(configuration)
    override val realmRepository = RealmRepository(configuration)
    override val rideLinkRepository = RideLinkRepository(configuration)
    override val rideRepository = RideRepository(configuration)
    override val shopOfferRepository = ShopOfferRepository(configuration)
    override val shopRepository = ShopRepository(configuration)
    override val slotMachineItemsRepository = SlotMachineItemsRepository(configuration)
    override val titleRepository = TitleRepository(configuration)
    override val warpRepository = WarpRepository(configuration)
    override val storeRepository = StoreRepository(configuration)
    override val audioServerLinkRepository = AudioServerLinkRepository(configuration)
    override val vipTrialRepository = VipTrialRepository(configuration)
    override val discordLinkRepository = DiscordLinkRepository(configuration)
    override val tagGroupRepository = TagGroupRepository(configuration)
    override val ftpUserRepository = FtpUserRepository(configuration)
    override val achievementProgressRepository = AchievementProgressRepository(configuration)
    override val activeCoinBoosterRepository = ActiveCoinBoosterRepository(configuration)
    override val activeServerCoinBoosterRepository = ActiveServerCoinBoosterRepository(configuration)
    override val bankAccountRepository = BankAccountRepository(configuration)
    override val guestStatRepository = GuestStatRepository(configuration)
    override val mailRepository = MailRepository(configuration)
    override val minigameScoreRepository = MinigameScoreRepository(configuration)
    override val playerEquippedItemRepository = PlayerEquippedItemRepository(configuration)
    override val playerKeyValueRepository = PlayerKeyValueRepository(configuration)
    override val playerOwnedItemRepository = PlayerOwnedItemRepository(configuration)
    override val rideCounterRepository = RideCounterRepository(configuration)
    override val uuidNameRepository = UuidNameRepository(configuration)
    override val aegisBlackListRideRepository = AegisBlackListRideRepository(configuration)
    override val luckPermsRepository = LuckPermsRepository(configuration, SQLDialect.MARIADB)
    override val rideLogRepository = RideLogRepository(configuration)
    override val transactionLogRepository = TransactionLogRepository(configuration)
    override val playerLocaleRepository = PlayerLocaleRepository(configuration)
    override val playerTimezoneRepository = PlayerTimezoneRepository(configuration)
    override val serverRepository = ServerRepository(configuration)
    override val teamScoreRepository = TeamScoreRepository(configuration)
    override val resourcePackRepository = ResourcePackRepository(configuration)
    override val teamScoreMemberRepository = TeamScoreMemberRepository(configuration)
    override val emojiRepository = EmojiRepository(configuration)
    override val donationRepository = DonationRepository(configuration)
    override val donationPackageRepository = DonationPackageRepository(configuration)
    override val tebexPackageRepository = TebexPackageRepository(configuration)

    override val allRepositories = listOfNotNull(
        achievementCategoryRepository,
        achievementRepository,
        casinoLogRepository,
        coinBoosterRepository,
        configRepository,
        cachedGameProfileRepository,
        itemStackDataRepository,
        mapEntriesRepository,
        ownableItemRepository,
        protectionRepository,
        realmRepository,
        rideRepository,
        rideLinkRepository,
        shopOfferRepository,
        shopRepository,
        slotMachineItemsRepository,
        titleRepository,
        warpRepository,
        storeRepository,
        audioServerLinkRepository,
        vipTrialRepository,
        discordLinkRepository,
        tagGroupRepository,
        ftpUserRepository,
        luckPermsRepository,
        achievementProgressRepository,
        activeCoinBoosterRepository,
        activeServerCoinBoosterRepository,
        bankAccountRepository,
        guestStatRepository,
        mailRepository,
        minigameScoreRepository,
        playerEquippedItemRepository,
        playerKeyValueRepository,
        playerOwnedItemRepository,
        rideCounterRepository,
        uuidNameRepository,
        aegisBlackListRideRepository,
        rideLogRepository,
        transactionLogRepository,
        playerLocaleRepository,
        playerTimezoneRepository,
        serverRepository,
        teamScoreRepository,
        resourcePackRepository,
        teamScoreMemberRepository,
        emojiRepository,
        donationRepository,
        donationPackageRepository,
        tebexPackageRepository,
    )
}