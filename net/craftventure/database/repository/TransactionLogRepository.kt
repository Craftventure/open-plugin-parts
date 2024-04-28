package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.TransactionLogDao
import net.craftventure.database.generated.cvdata.tables.pojos.TransactionLog
import net.craftventure.database.generated.cvdata.tables.records.TransactionLogRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class TransactionLogRepository(
    configuration: Configuration
) : BaseIdRepository<TransactionLogRecord, TransactionLog, UUID>(
    TransactionLogDao(configuration)
) {
    fun insertSilent(log: TransactionLog) = withDslIgnoreErrors(0) { dsl ->
        val record = dsl.newRecord(Cvdata.CVDATA.TRANSACTION_LOG, log)
        record.store()
    }

    fun cleanup(): Int = withDslIgnoreErrors(0) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.TRANSACTION_LOG.DATE.lt(LocalDateTime.now().minusDays(30)))
            .execute()
    }

    fun findAllForUuid(uuid: UUID): List<TransactionLog> = withDsl { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.TRANSACTION_LOG.UUID.eq(uuid))
            .fetchInto(TransactionLog::class.java)
    }

//    fun getEconomyDelta(start: Date, stop: Date): GenericRawResults<Array<String>>? {
//        try {
//            val results =
//                dao.queryRaw("SELECT sum(delta) from transactionlog WHERE \"date\" >= ${start.time} and \"date\" <= ${stop.time}\n")
//            return results
//        } catch (e: SQLException) {
//            Logger.capture(e)
//        }
//
//        return null
//    }
}