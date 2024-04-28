package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.extension.execute
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.PlayerEquippedItemDao
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerEquippedItem
import net.craftventure.database.generated.cvdata.tables.records.PlayerEquippedItemRecord
import net.craftventure.database.type.EquippedItemSlot
import org.jooq.Configuration
import java.util.*

class PlayerEquippedItemRepository(
    configuration: Configuration
) : BaseIdRepository<PlayerEquippedItemRecord, PlayerEquippedItem, UUID>(
    PlayerEquippedItemDao(configuration),
) {
    fun reset(uuid: UUID) = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.UUID.eq(uuid))
            .execute()
        true
    }

    @Deprecated(message = "Prefer calling with an OwnableItem instance")
    fun update(playerId: UUID, slot: EquippedItemSlot, itemId: String?, sourceOwnedItem: UUID?): Boolean {
        val existing = withDslIgnoreErrors(null) { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.UUID.eq(playerId))
                .and(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.SLOT.eq(slot))
                .fetchOneInto(dao.type)
        }
        if (itemId == null) {
            return withDslIgnoreErrors(null) { dsl ->
                dsl.deleteFrom(table)
                    .where(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.UUID.eq(playerId))
                    .and(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.SLOT.eq(slot))
                    .execute {
                        if (existing != null)
                            triggerListenerDelete(existing)
                    }
            } == 1
        }

        val updateBuilder = configuration.dsl()
            .update(table)
            .set(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.ITEM, itemId)
            .set(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.SOURCE, sourceOwnedItem)
            .where(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.UUID.eq(playerId))
            .and(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.SLOT.eq(slot))

        val toClear = EquippedItemSlot.clears[slot]

        if (toClear != null)
            Logger.debug("Clearing equipment ${toClear.joinToString(", ")}")

        toClear?.forEach { slotToClear ->
            withDslIgnoreErrors(0) { dsl ->
                dsl.deleteFrom(table)
                    .where(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.UUID.eq(playerId))
                    .and(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.SLOT.eq(slotToClear))
                    .execute()
            }
        }

        try {
            if (updateBuilder.execute() == 1) {
                if (existing != null)
                    triggerListenerUpdate(existing)
                return true
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }

        val equippedItem = PlayerEquippedItem(
            id = UUID.randomUUID(),
            uuid = playerId,
            slot = slot,
            item = itemId,
            source = sourceOwnedItem,
        )
        return create(equippedItem)
    }

    fun getAllByPlayer(uuid: UUID): List<PlayerEquippedItem> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_EQUIPPED_ITEM.UUID.eq(uuid))
            .fetchInto(dao.type)
    }
}