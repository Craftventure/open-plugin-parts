package net.craftventure.database.repository

import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.PlayerKeyValueDao
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerKeyValue
import net.craftventure.database.generated.cvdata.tables.records.PlayerKeyValueRecord
import net.craftventure.database.generated.cvdata.tables.references.PLAYER_KEY_VALUE
import org.jooq.Configuration
import java.util.*

class PlayerKeyValueRepository(
    configuration: Configuration
) : BaseIdRepository<PlayerKeyValueRecord, PlayerKeyValue, UUID>(
    PlayerKeyValueDao(configuration),
) {
    fun reset(uuid: UUID) = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.PLAYER_KEY_VALUE.UUID.eq(uuid))
            .execute()
        true
    }

    fun update(uuid: UUID, key: String, value: String?): Boolean =
        if (value != null) {
            withDslIgnoreErrors(false) { dsl ->
                dsl.update(table)
                    .set(Cvdata.CVDATA.PLAYER_KEY_VALUE.VALUE, value)
                    .where(Cvdata.CVDATA.PLAYER_KEY_VALUE.UUID.eq(uuid))
                    .and(Cvdata.CVDATA.PLAYER_KEY_VALUE.KEY.eq(key))
                    .execute() == 1
            }
        } else {
            deleteByKey(uuid, key)
        }

    fun deleteByKey(uuid: UUID, key: String) = withDslIgnoreErrors(false) { dsl ->
        val delete = dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_KEY_VALUE.UUID.eq(uuid))
            .and(Cvdata.CVDATA.PLAYER_KEY_VALUE.KEY.eq(key))
            .fetchInto(dao.type)
        (dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.PLAYER_KEY_VALUE.UUID.eq(uuid))
            .and(Cvdata.CVDATA.PLAYER_KEY_VALUE.KEY.eq(key))
            .execute() == 1)
            .also {
                if (it) {
                    triggerListenerDelete(delete)
                }
            }
    }

    operator fun get(uuid: UUID, key: String): PlayerKeyValue? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_KEY_VALUE.UUID.eq(uuid))
            .and(Cvdata.CVDATA.PLAYER_KEY_VALUE.KEY.eq(key))
            .fetchOneInto(dao.type)
    }

    fun getAllValues(key: String) = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(PLAYER_KEY_VALUE.KEY.eq(key))
            .fetchInto(dao.type)
    }

    fun getValue(uuid: UUID, key: String): String? = get(uuid, key)?.value

    fun getAllForPlayer(player: UUID) = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_KEY_VALUE.UUID.eq(player))
            .fetchInto(dao.type)
    }

    fun create(uuid: UUID, key: String, value: String?): Boolean = createSilent(
        PlayerKeyValue(
            UUID.randomUUID(),
            uuid,
            key,
            value
        )
    )

    fun createOrUpdate(uuid: UUID, key: String, value: String?): Boolean = withDslIgnoreErrors(false) { dsl ->
        val updated = dsl.update(table)
            .set(PLAYER_KEY_VALUE.VALUE, value)
            .where(PLAYER_KEY_VALUE.UUID.eq(uuid))
            .and(PLAYER_KEY_VALUE.KEY.eq(key))
            .execute() == 1

        if (updated) {
            triggerListenerUpdate(
                dsl.selectFrom(table)
                    .where(PLAYER_KEY_VALUE.UUID.eq(uuid))
                    .and(PLAYER_KEY_VALUE.KEY.eq(key)).fetchInto(dao.type)
            )
            return@withDslIgnoreErrors true
        } else {
            return@withDslIgnoreErrors create(uuid, key, value)
        }
    }

    fun getHighestValues(key: String, limit: Long = 3): List<PlayerKeyValue> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PLAYER_KEY_VALUE.KEY.eq(key))
            .and(Cvdata.CVDATA.PLAYER_KEY_VALUE.UUID.notIn(MainRepositoryProvider.aegisBlackListRideRepository.cachedItems.map { it.uuid }))
            .orderBy(Cvdata.CVDATA.PLAYER_KEY_VALUE.VALUE.cast(Double::class.java).desc())
            .limit(limit)
            .fetchInto(dao.type)
    }

    companion object {
        const val KEY_ADMIN_CHAT = "adminchat"
        const val KEY_MESSAGE_SPY = "spy_message"
        const val KEY_MESSAGE_BLOCKED = "message_blocked"
        const val DISTANCE_TRAVELED_BY_TRAIN = "distance_by_train"
        const val DISTANCE_TRAVELED_BY_TRAM = "distance_by_tram"
        const val ROK_EGG_UNUSED_COUNT = "rok_egg_unused"
    }
}