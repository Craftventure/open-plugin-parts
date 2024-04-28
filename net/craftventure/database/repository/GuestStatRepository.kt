package net.craftventure.database.repository

import net.craftventure.core.ktx.extension.toUuid
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.GuestStatDao
import net.craftventure.database.generated.cvdata.tables.pojos.GuestStat
import net.craftventure.database.generated.cvdata.tables.records.GuestStatRecord
import org.jooq.Configuration
import org.jooq.impl.DSL
import java.time.LocalDateTime
import java.util.*


class GuestStatRepository(
    configuration: Configuration
) : BaseIdRepository<GuestStatRecord, GuestStat, UUID>(
    GuestStatDao(configuration),
) {
    fun updateName(uuid: UUID, name: String) = withDslIgnoreErrors(false) { dsl ->
        dsl.update(table)
            .set(Cvdata.CVDATA.GUEST_STAT.LAST_KNOWN_NAME, name)
            .set(Cvdata.CVDATA.GUEST_STAT.LAST_SEEN, LocalDateTime.now())
            .where(Cvdata.CVDATA.GUEST_STAT.UUID.eq(uuid))
            .execute() == 1
    }

    fun deltaOnlineAndAfkTimes(uuid: UUID, delta: Long, afkDelta: Long) = withDslIgnoreErrors(false) { dsl ->
        dsl.update(table)
            .set(Cvdata.CVDATA.GUEST_STAT.TOTAL_ONLINE_TIME, Cvdata.CVDATA.GUEST_STAT.TOTAL_ONLINE_TIME.plus(delta))
            .set(Cvdata.CVDATA.GUEST_STAT.TOTAL_AFK_TIME, Cvdata.CVDATA.GUEST_STAT.TOTAL_AFK_TIME.plus(afkDelta))
            .set(Cvdata.CVDATA.GUEST_STAT.LAST_SEEN, LocalDateTime.now())
            .where(Cvdata.CVDATA.GUEST_STAT.UUID.eq(uuid))
            .execute() == 1
    }

    fun lastSeenCountBetween(start: LocalDateTime, end: LocalDateTime): Int = withDslIgnoreErrors(0) { dsl ->
        dsl.fetchCount(
            table,
            Cvdata.CVDATA.GUEST_STAT.LAST_SEEN.ge(start),
            Cvdata.CVDATA.GUEST_STAT.LAST_SEEN.le(end)
        )
    }

    fun firstSeenCountBetween(start: LocalDateTime, end: LocalDateTime): Int = withDslIgnoreErrors(0) { dsl ->
        dsl.fetchCount(
            table,
            Cvdata.CVDATA.GUEST_STAT.FIRST_SEEN.ge(start),
            Cvdata.CVDATA.GUEST_STAT.FIRST_SEEN.le(end)
        )
    }

    fun firstSeenBetween(start: LocalDateTime, end: LocalDateTime): List<GuestStat> =
        withDslIgnoreErrors(emptyList()) { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.GUEST_STAT.FIRST_SEEN.ge(start))
                .and(Cvdata.CVDATA.GUEST_STAT.FIRST_SEEN.le(end))
                .fetchInto(dao.type)
        }

    fun getRowId(uuid: UUID): Long? = withDslIgnoreErrors(null, configuration = configuration.derive().apply {
//        set(object : DefaultExecuteListener() {
//            override fun renderEnd(ctx: ExecuteContext) {
//                ctx.sql("SET @rank = 0;" + ctx.sql())
//            }
//        })
    }) { dsl ->
        dsl.execute("SET @rank=0")
        val rankFieldName = DSL.field("rank", Int::class.java)
        val rankFieldCounter = DSL.field("@rank:=@rank+1", Int::class.java).`as`(rankFieldName)

        val orderedList = dsl.select(rankFieldCounter, Cvdata.CVDATA.GUEST_STAT.UUID)
            .from(table)
            .orderBy(Cvdata.CVDATA.GUEST_STAT.FIRST_SEEN.asc())

        val result = dsl.select(rankFieldName)
            .from(orderedList)
            .where(orderedList.field(Cvdata.CVDATA.GUEST_STAT.UUID)!!.eq(uuid))
            .fetchOne()

        return@withDslIgnoreErrors result?.get(rankFieldName)?.toLong()
    }

    fun getByName(name: String): GuestStat? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.GUEST_STAT.LAST_KNOWN_NAME.eq(name))
            .orderBy(Cvdata.CVDATA.GUEST_STAT.LAST_SEEN.desc())
            .limit(1)
            .fetchOneInto(dao.type)
    }

    fun getByNameOrUuid(nameOrUuid: String): GuestStat? = getByName(nameOrUuid) ?: try {
        nameOrUuid.toUuid()?.let { findSilent(it) }
    } catch (e: Exception) {
//        Logger.capture(e)
        null
    }

    fun createIfNotExists(uuid: UUID, name: String?): GuestStat? = withDslIgnoreErrors(null) { dsl ->
        findSilent(uuid) ?: GuestStat(
            uuid,
            name ?: "",
            0,
            0,
            LocalDateTime.now(),
            LocalDateTime.now()
        ).let {
            if (createOrUpdateSilent(it)) it
            else null
        }
    }
}