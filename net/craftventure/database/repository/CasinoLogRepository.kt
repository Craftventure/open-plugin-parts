package net.craftventure.database.repository

import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.pojos.CasinoLog
import net.craftventure.database.generated.cvdata.tables.records.CasinoLogRecord
import org.jooq.Configuration
import org.jooq.SQLDialect
import java.util.*

class CasinoLogRepository(
    configuration: Configuration
) : BaseRepository<CasinoLogRecord>(
    configuration,
    Cvdata.CVDATA.CASINO_LOG,
    SQLDialect.MARIADB
) {
    @Deprecated(message = "Only for migration")
    fun insert(items: List<CasinoLog>) = withDsl { dsl ->
        val mapped = items.map { dsl.newRecord(Cvdata.CVDATA.CASINO_LOG, it) }
        dsl.batchInsert(mapped).execute()
    }

    fun createOrUpdate(uuid: UUID, type: String, vcDelta: Int): Boolean {
        try {
            return withDsl { dsl ->
                val updated = dsl.update(table)
                    .set(Cvdata.CVDATA.CASINO_LOG.VC, Cvdata.CVDATA.CASINO_LOG.VC.plus(vcDelta))
                    .set(Cvdata.CVDATA.CASINO_LOG.TIMES_PLAYED, Cvdata.CVDATA.CASINO_LOG.TIMES_PLAYED.plus(1))
                    .execute() == 1
                if (updated) return@withDsl true

                val log = CasinoLog(UUID.randomUUID(), uuid, type, vcDelta, 1)
                val record = dsl.newRecord(Cvdata.CVDATA.CASINO_LOG, log)
                return@withDsl record.store() == 1
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun findByPlayer(uuid: UUID) = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.CASINO_LOG.UUID.eq(uuid))
            .fetchInto(CasinoLog::class.java)
    }

    fun getTopRewards(machineType: String, limit: Long = 10) = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.CASINO_LOG.TYPE.eq(machineType))
            .and(Cvdata.CVDATA.CASINO_LOG.UUID.notIn(MainRepositoryProvider.aegisBlackListRideRepository.cachedItems.map { it.uuid }))
            .orderBy(Cvdata.CVDATA.CASINO_LOG.VC.desc())
            .limit(limit)
            .fetchInto(CasinoLog::class.java)
    }
}