package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.async.executeAsync
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.BankAccountType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player


class ManagedSideScoreBoard(
    val player: Player
) : BasePlayerMetadata(player) {
    var vc: Long = 0
        private set

    init {
        updateAll()
    }

    override fun debugComponent() = Component.text("vc=$vc")

    fun updateAll() {
        updateCoinDisplay()
    }

    fun updateCoinDisplay() {
        updateVentureCoinDisplay()
    }

    fun updateCoinDisplay(bankAccountType: BankAccountType) {
        when (bankAccountType) {
            BankAccountType.VC -> updateVentureCoinDisplay()
//            BankAccountType.WINTERCOIN -> updateWinterCoinDisplay()
//            BankAccountType.WINTER_TICKETS -> updateWinterTicketDisplay()
            else -> {}
        }
    }

    fun updateVentureCoinDisplay() {
        executeAsync {
            vc = (MainRepositoryProvider.bankAccountRepository.getOrCreate(player.uniqueId, BankAccountType.VC)?.balance
                ?: 0)
        }
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { ManagedSideScoreBoard(player) }
    }
}
