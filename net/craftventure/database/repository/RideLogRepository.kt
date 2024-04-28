package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.pojos.RideLog
import net.craftventure.database.generated.cvdata.tables.records.RideLogRecord
import org.jooq.Configuration
import org.jooq.SQLDialect
import java.time.LocalDateTime
import java.util.*

class RideLogRepository(
    configuration: Configuration
) : BaseRepository<RideLogRecord>(
    configuration,
    Cvdata.CVDATA.RIDE_LOG,
    SQLDialect.MARIADB
) {
    fun insertSilent(log: RideLog) = withDslIgnoreErrors(0) { dsl ->
        val record = dsl.newRecord(Cvdata.CVDATA.RIDE_LOG, log)
        record.store()
    }

    fun cleanup(): Int = withDslIgnoreErrors(0) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.RIDE_LOG.DATE.lt(LocalDateTime.now().minusDays(30)))
            .execute()
    }

    fun findAllForUuid(uuid: UUID): List<RideLog> = withDsl { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.RIDE_LOG.UUID.eq(uuid))
            .fetchInto(RideLog::class.java)
    }

//    fun getMostCompletedRide(start: LocalDateTime, stop: LocalDateTime): GenericRawResults<Array<String>>? {
//        try {
//            val results = dao.queryRaw(
//                "SELECT ride, COUNT(ride) as cnt\n" +
//                        "FROM ridelog\n" +
//                        "WHERE \"rideLogState\" = \"COMPLETED\" and \"date\" >= ${start.time} and \"date\" <= ${stop.time}\n" +
//                        "GROUP BY \"ride\"\n" +
//                        "ORDER BY cnt DESC LIMIT 2;"
//            )
//            return results
//        } catch (e: SQLException) {
//            Logger.capture(e)
//        }
//
//        return null
//    }
//
//    fun getMostLeftRide(start: LocalDateTime, stop: LocalDateTime): GenericRawResults<Array<String>>? {
//        try {
//            val results = dao.queryRaw(
//                "SELECT ride, COUNT(ride) as cnt\n" +
//                        "FROM ridelog\n" +
//                        "WHERE \"rideLogState\" = \"LEFT\" and \"date\" >= ${start.time} and \"date\" <= ${stop.time}\n" +
//                        "GROUP BY \"ride\"\n" +
//                        "ORDER BY cnt DESC LIMIT 2;"
//            )
//            return results
//        } catch (e: SQLException) {
//            Logger.capture(e)
//        }
//
//        return null
//    }
}