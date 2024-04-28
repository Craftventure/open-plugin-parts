package net.craftventure.temporary

import net.craftventure.bukkit.ktx.entitymeta.MetaAnnotations
import net.craftventure.core.async.executeSync
import net.craftventure.core.metadata.ManagedSideScoreBoard
import net.craftventure.core.serverevent.BankAccountUpdateEvent
import net.craftventure.database.generated.cvdata.tables.pojos.BankAccount
import net.craftventure.database.repository.BaseIdRepository
import org.bukkit.Bukkit

class BankAccountListener : BaseIdRepository.Listener<BankAccount>() {
    override fun onMerge(item: BankAccount) {
        handle(item)
    }

    override fun onInsert(item: BankAccount) {
        handle(item)
    }

    override fun onUpdate(item: BankAccount) {
        handle(item)
    }

    override fun onDelete(item: BankAccount) {
        updateDisplay(item)
    }

    private fun handle(item: BankAccount) {
        val player = Bukkit.getPlayer(item.uuid!!)
        if (player != null) {
            executeSync {
                Bukkit.getServer().pluginManager.callEvent(
                    BankAccountUpdateEvent(
                        player,
                        item.type!!,
                        item.balance!!
                    )
                )
            }
        }
        updateDisplay(item)
    }

    private fun updateDisplay(item: BankAccount) {
        val player = Bukkit.getPlayer(item.uuid!!)
        if (player != null) {
            MetaAnnotations.getMetadata(player, ManagedSideScoreBoard::class.java)?.updateCoinDisplay(item.type!!)
        }
    }
}