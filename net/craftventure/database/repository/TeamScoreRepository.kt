package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.TeamScoreDao
import net.craftventure.database.generated.cvdata.tables.pojos.TeamScore
import net.craftventure.database.generated.cvdata.tables.records.TeamScoreRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class TeamScoreRepository(
    configuration: Configuration
) : BaseIdRepository<TeamScoreRecord, TeamScore, UUID>(
    TeamScoreDao(configuration)
) {
    fun getTopCounters(
        target: String,
        limit: Long = 10,
        after: LocalDateTime? = null,
        before: LocalDateTime? = null,
    ): List<TeamScore> = withDslIgnoreErrors(emptyList()) { dsl ->
//        if (true) return@withDslIgnoreErrors emptyList()
        var queryBuilder = dsl.selectFrom(table)
            .where(Cvdata.CVDATA.TEAM_SCORE.SCORE.ge(0))
            .and(Cvdata.CVDATA.TEAM_SCORE.TARGET.eq(target))

        if (after != null)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.TEAM_SCORE.AT.ge(after))

        if (before != null)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.TEAM_SCORE.AT.le(before))

        queryBuilder
//            .groupBy(Cvdata.CVDATA.TEAM_SCORE.UUID)
            .orderBy(Cvdata.CVDATA.TEAM_SCORE.SCORE.desc())
            .limit(limit)
//            .apply {
//                Logger.debug(sql)
//            }
            .fetchInto(dao.type)/*.also {
                Logger.debug("For TopCounters $gameId limit=$limit type=$type asc=$scoreAscending crew=$includeCrew $before<->$after aggr=$scoreAggregate $uuids")
                Logger.debug("Result ${it.size}:\n- ${it.joinToString("\n- ") { "${it.uuid} -> ${it.score}" }}")
            }*/
    }
}