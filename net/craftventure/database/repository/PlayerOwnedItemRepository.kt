package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.PlayerOwnedItem.Companion.PLAYER_OWNED_ITEM
import net.craftventure.database.generated.cvdata.tables.daos.PlayerOwnedItemDao
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.database.generated.cvdata.tables.records.PlayerOwnedItemRecord
import net.craftventure.database.wrap.WrappedPlayerOwnedItem
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class PlayerOwnedItemRepository(
    configuration: Configuration
) : BaseIdRepository<PlayerOwnedItemRecord, PlayerOwnedItem, UUID>(
    PlayerOwnedItemDao(configuration),
) {
    private val virtualItemProviders = hashSetOf<VirtualItemProvider>()

    fun reset(uuid: UUID) = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
            .execute()
        true
    }

    fun addVirtualItemProvider(instance: VirtualItemProvider) {
        virtualItemProviders.add(instance)
    }

    fun removeVirtualItemProvider(instance: VirtualItemProvider) {
        virtualItemProviders.remove(instance)
    }

    fun changePaidPrice(uuid: UUID, itemId: String, paidPrice: Int, newPrice: Int): Int =
        withDslIgnoreErrors(0) { dsl ->
            dsl.update(table)
                .set(Cvdata.CVDATA.PLAYER_OWNED_ITEM.PAID_PRICE, newPrice)
                .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
                .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(itemId))
                .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.PAID_PRICE.eq(paidPrice))
                .execute()
        }

    fun totalOwnCount(itemId: String): Long = withDslIgnoreErrors(0L) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(itemId))
            .count().toLong()
    }

    fun findOwnersOfItem(itemId: String): List<PlayerOwnedItem> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(itemId))
            .fetchInto(dao.type)
    }

    fun owns(uuid: UUID, ownedItemId: String?): Boolean = withDslIgnoreErrors(false) { dsl ->
        if (ownedItemId == null) return@withDslIgnoreErrors false
        if (virtualItemProviders.any { it.providesVirtualItem(uuid, ownedItemId) }) return@withDslIgnoreErrors true
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
            .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(ownedItemId))
            .count() > 0
    }

    fun ownsCount(uuid: UUID, ownedItemId: String): Int = withDslIgnoreErrors(0) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
            .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(ownedItemId))
            .count()
    }

    fun get(uuid: UUID): List<PlayerOwnedItem> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
            .fetchInto(dao.type)
    }

    fun get(uuid: UUID, itemId: String): List<PlayerOwnedItem> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
            .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(itemId))
            .fetchInto(dao.type)
    }

//    fun delete(owner: UUID, ownableItem: String, itemId: UUID? = null): Int =
//        withDslIgnoreErrors(0) { dsl ->
//            val query = if (itemId != null) dsl.deleteFrom(table)
//                .where(Tables.PLAYER_OWNED_ITEM.ID.eq(itemId))
//            else
//                dsl.deleteFrom(table)
//                    .where(Tables.PLAYER_OWNED_ITEM.UUID.eq(owner))
//                    .and(Tables.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(ownableItem))
//
//            val result = query.execute()
//            if (result == 1) {
//
//            }
//            result
//        }

    fun create(
        uuid: UUID,
        ownableItemId: String,
        paidPrice: Int,
        times: Int,
        disableListeners: Boolean = false,
    ): Boolean {
        val items = (0 until times).map {
            PlayerOwnedItem(
                UUID.randomUUID(),
                uuid,
                ownableItemId,
                LocalDateTime.now(),
                paidPrice,
                null
            )
        }
        try {
            dao.insert(items)
            if (!disableListeners)
                triggerListenerCreate(items)
            return true
        } catch (e: Exception) {
            Logger.capture(e)
        }
        return false
    }

    fun createOneLimited(
        uuid: UUID,
        ownableItemId: String,
        paidPrice: Int,
    ): Boolean = withDslIgnoreErrors(false) { dsl ->
        val exists = dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
            .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(ownableItemId))
            .count() > 0
        if (exists) return@withDslIgnoreErrors false
        val playerOwnedItem = PlayerOwnedItem(
            UUID.randomUUID(),
            uuid,
            ownableItemId,
            LocalDateTime.now(),
            paidPrice,
            null
        )
        return@withDslIgnoreErrors createSilent(playerOwnedItem)
    }

    fun delete(uuid: UUID, ownableItemId: String, limit: Int): Int =
        withDslIgnoreErrors(0) { dsl ->
            val delete = dsl.selectFrom(table)
                .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
                .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(ownableItemId))
                .limit(limit)
                .fetchInto(dao.type)

            val deleted = dsl.deleteFrom(table)
                .where(Cvdata.CVDATA.PLAYER_OWNED_ITEM.UUID.eq(uuid))
                .and(Cvdata.CVDATA.PLAYER_OWNED_ITEM.OWNED_ITEM_ID.eq(ownableItemId))
                .limit(limit)
                .execute()

            if (deleted > 0) {
                triggerListenerDelete(delete)
            }
            deleted
        }

    fun updateOwnableItemMetadata(uniqueItemId: UUID, metadataJson: String) = withDslIgnoreErrors(0) { dsl ->
        dsl.update(table)
            .set(PLAYER_OWNED_ITEM.METADATA, metadataJson)
            .where(PLAYER_OWNED_ITEM.ID.eq(uniqueItemId))
            .execute()
    }

    interface VirtualItemProvider {
        fun provideVirtualItems(who: UUID): List<WrappedPlayerOwnedItem>
        fun providesVirtualItem(who: UUID, itemId: String): Boolean =
            provideVirtualItems(who).any { it.source.ownedItemId == itemId }
    }
}