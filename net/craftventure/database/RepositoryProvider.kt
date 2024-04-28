package net.craftventure.database

import net.craftventure.database.repository.*
import org.jooq.impl.TableRecordImpl

interface RepositoryProvider {
    val achievementCategoryRepository: AchievementCategoryRepository
    val achievementRepository: AchievementRepository
    val casinoLogRepository: CasinoLogRepository
    val coinBoosterRepository: CoinBoosterRepository
    val configRepository: ConfigRepository
    val cachedGameProfileRepository: CachedGameProfileRepository
    val itemStackDataRepository: ItemStackDataRepository
    val mapEntriesRepository: MapEntriesRepository
    val ownableItemRepository: OwnableItemRepository
    val protectionRepository: ProtectionRepository
    val realmRepository: RealmRepository
    val rideLinkRepository: RideLinkRepository
    val rideRepository: RideRepository
    val shopOfferRepository: ShopOfferRepository
    val shopRepository: ShopRepository
    val slotMachineItemsRepository: SlotMachineItemsRepository
    val titleRepository: TitleRepository
    val warpRepository: WarpRepository
    val storeRepository: StoreRepository
    val audioServerLinkRepository: AudioServerLinkRepository
    val vipTrialRepository: VipTrialRepository
    val discordLinkRepository: DiscordLinkRepository
    val tagGroupRepository: TagGroupRepository
    val ftpUserRepository: FtpUserRepository
    val achievementProgressRepository: AchievementProgressRepository
    val activeCoinBoosterRepository: ActiveCoinBoosterRepository
    val activeServerCoinBoosterRepository: ActiveServerCoinBoosterRepository
    val bankAccountRepository: BankAccountRepository
    val guestStatRepository: GuestStatRepository
    val mailRepository: MailRepository
    val minigameScoreRepository: MinigameScoreRepository
    val playerEquippedItemRepository: PlayerEquippedItemRepository
    val playerKeyValueRepository: PlayerKeyValueRepository
    val playerOwnedItemRepository: PlayerOwnedItemRepository
    val rideCounterRepository: RideCounterRepository
    val uuidNameRepository: UuidNameRepository
    val luckPermsRepository: LuckPermsRepository
    val aegisBlackListRideRepository: AegisBlackListRideRepository
    val rideLogRepository: RideLogRepository
    val transactionLogRepository: TransactionLogRepository
    val playerLocaleRepository: PlayerLocaleRepository
    val playerTimezoneRepository: PlayerTimezoneRepository
    val serverRepository: ServerRepository
    val teamScoreRepository: TeamScoreRepository
    val teamScoreMemberRepository: TeamScoreMemberRepository
    val resourcePackRepository: ResourcePackRepository
    val emojiRepository: EmojiRepository
    val donationRepository: DonationRepository
    val donationPackageRepository: DonationPackageRepository
    val tebexPackageRepository: TebexPackageRepository
    val allRepositories: List<BaseRepository<out TableRecordImpl<*>>>
}