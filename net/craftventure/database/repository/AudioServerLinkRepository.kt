package net.craftventure.database.repository

import net.craftventure.database.GfyIdGenerator
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.AudioserverLinkDao
import net.craftventure.database.generated.cvdata.tables.pojos.AudioserverLink
import net.craftventure.database.generated.cvdata.tables.records.AudioserverLinkRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class AudioServerLinkRepository(
    configuration: Configuration
) : BaseIdRepository<AudioserverLinkRecord, AudioserverLink, UUID>(
    AudioserverLinkDao(configuration)
) {
    fun isValid(player: UUID, key: String?): Boolean {
        if (key == null)
            return false
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.AUDIOSERVER_LINK.MINECRAFT_UUID.eq(player))
                .and(Cvdata.CVDATA.AUDIOSERVER_LINK.KEY.eq(key))
                .query
                .count() == 1
        }
    }

    fun reset(player: UUID): Boolean {
        return withDsl { dsl ->
            dsl.deleteFrom(table)
                .where(Cvdata.CVDATA.AUDIOSERVER_LINK.MINECRAFT_UUID.eq(player))
                .execute() >= 0
        }
    }

    fun getOrCreate(player: UUID): AudioserverLink? {
        return withDsl { dsl ->
            val existingRecord = dsl.selectFrom(table)
                .where(Cvdata.CVDATA.AUDIOSERVER_LINK.MINECRAFT_UUID.eq(player))
                .query
                .fetchInto(AudioserverLink::class.java)?.firstOrNull()
            if (existingRecord?.key != null) {
                return@withDsl existingRecord
            }
            val record = dsl.newRecord(Cvdata.CVDATA.AUDIOSERVER_LINK).apply {
                minecraftUuid = player
                key = GfyIdGenerator.generateCombination(2, "")
                createdAt = LocalDateTime.now()
            }
            val insert = dsl.insertInto(table)
                .set(record.intoMap())
                .onDuplicateKeyIgnore()
                .execute()
            if (insert == 1)
                return@withDsl AudioserverLink(record.minecraftUuid, record.key, record.createdAt)
            null
        }
    }
}