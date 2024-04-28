package net.craftventure.temporary

import net.craftventure.bukkit.ktx.entitymeta.MetaAnnotations
import net.craftventure.bukkit.ktx.extension.player
import net.craftventure.core.metadata.ManagedSideScoreBoard
import net.craftventure.core.task.ActiveMoneyRewardTask
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveCoinBooster
import net.craftventure.database.repository.BaseIdRepository
import net.craftventure.database.type.BankAccountType

class ActiveCoinBoosterActivationListener : BaseIdRepository.Listener<ActiveCoinBooster>() {
    override fun onMerge(item: ActiveCoinBooster) {
        handle(item)
    }

    override fun onInsert(item: ActiveCoinBooster) {
        handle(item)
    }

    override fun onUpdate(item: ActiveCoinBooster) {
        handle(item)
    }

    private fun handle(item: ActiveCoinBooster) {
        ActiveMoneyRewardTask.onActivated(item)

        val player = item.uuid?.player ?: return
        MetaAnnotations.getMetadata(player, ManagedSideScoreBoard::class.java)
            ?.updateCoinDisplay(BankAccountType.VC)
    }
}