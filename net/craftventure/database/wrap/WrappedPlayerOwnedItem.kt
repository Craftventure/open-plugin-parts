package net.craftventure.database.wrap

import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import java.time.LocalDateTime
import java.util.*

data class WrappedPlayerOwnedItem(
    val source: PlayerOwnedItem
) {
    val id: UUID?
        get() = source.id
    val uuid: UUID?
        get() = source.uuid
    val ownedItemId: String?
        get() = source.ownedItemId
    val buyDate: LocalDateTime?
        get() = source.buyDate
    val paidPrice: Int?
        get() = source.paidPrice
    val metadata: String?
        get() = source.metadata

    var isVirtual: Boolean = false

    companion object {
        fun PlayerOwnedItem.wrap() = WrappedPlayerOwnedItem(this)
        fun List<PlayerOwnedItem>.wrap() = map { WrappedPlayerOwnedItem(it) }
    }
}