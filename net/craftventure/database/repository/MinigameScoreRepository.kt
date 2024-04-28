package net.craftventure.database.repository

import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.MinigameScoreDao
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.generated.cvdata.tables.records.MinigameScoreRecord
import net.craftventure.database.type.MinigameScoreType
import org.jooq.Configuration
import org.jooq.impl.DSL
import java.time.LocalDateTime
import java.util.*

class MinigameScoreRepository(
    configuration: Configuration
) : BaseIdRepository<MinigameScoreRecord, MinigameScore, UUID>(
    MinigameScoreDao(configuration),
) {
    fun findByPlayerAndGame(uuid: UUID, game: String): MinigameScore? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.MINIGAME_SCORE.UUID.eq(uuid))
            .and(Cvdata.CVDATA.MINIGAME_SCORE.GAME.eq(game))
            .fetchOneInto(dao.type)
    }

    fun find(uuid: UUID, game: String, type: MinigameScoreType): MinigameScore? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.MINIGAME_SCORE.UUID.eq(uuid))
            .and(Cvdata.CVDATA.MINIGAME_SCORE.GAME.eq(game))
            .and(Cvdata.CVDATA.MINIGAME_SCORE.TYPE.eq(type))
            .fetchOneInto(dao.type)
    }


    fun deltaScore(uuid: UUID, game: String, type: MinigameScoreType, scoreDelta: Long): Int =
        withDslIgnoreErrors(0) { dsl ->
            dsl.update(table)
                .set(Cvdata.CVDATA.MINIGAME_SCORE.SCORE, Cvdata.CVDATA.MINIGAME_SCORE.SCORE.plus(scoreDelta))
                .where(Cvdata.CVDATA.MINIGAME_SCORE.UUID.eq(uuid))
                .and(Cvdata.CVDATA.MINIGAME_SCORE.GAME.eq(game))
                .and(Cvdata.CVDATA.MINIGAME_SCORE.TYPE.eq(type))
                .execute()
        }

    fun getTopCounters(
        gameId: String?,
        limit: Long = 10,
        type: MinigameScoreType? = null,
        scoreAscending: Boolean = false,
        includeCrew: Boolean = false,
        after: LocalDateTime? = null,
        before: LocalDateTime? = null,
        scoreAggregate: ScoreAggregate = ScoreAggregate.MIN,
        uuids: List<UUID>? = null
    ): List<MinigameScore> = withDslIgnoreErrors(emptyList()) { dsl ->
//        if (true) return@withDslIgnoreErrors emptyList()
        val fields = Cvdata.CVDATA.MINIGAME_SCORE.fields().toMutableList()
        val scoreFieldPlain = Cvdata.CVDATA.MINIGAME_SCORE.SCORE
        val scoreFieldSorted = when (scoreAggregate) {
            ScoreAggregate.MIN -> DSL.min(scoreFieldPlain).`as`(scoreFieldPlain.name)
            ScoreAggregate.MAX -> DSL.max(scoreFieldPlain).`as`(scoreFieldPlain.name)
        }
        fields.replaceAll { it ->
            if (it.name == scoreFieldPlain.name) {
//                Logger.debug("Replacing field ${it.name} (${scoreAggregate})")
                scoreFieldSorted
            } else it
        }

        var queryBuilder = dsl.select(fields)// dsl.select(*fields.toTypedArray())
            .from(table)
            .where(Cvdata.CVDATA.MINIGAME_SCORE.UUID.isNotNull)

        if (!includeCrew)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.MINIGAME_SCORE.CREW.eq(false))

        if (uuids != null)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.MINIGAME_SCORE.UUID.`in`(uuids))

        if (type != null)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.MINIGAME_SCORE.TYPE.eq(type))

        if (gameId != null)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.MINIGAME_SCORE.GAME.eq(gameId))

        if (after != null)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.MINIGAME_SCORE.AT.ge(after))

        if (before != null)
            queryBuilder = queryBuilder.and(Cvdata.CVDATA.MINIGAME_SCORE.AT.le(before))

        queryBuilder
            .and(Cvdata.CVDATA.MINIGAME_SCORE.UUID.notIn(MainRepositoryProvider.aegisBlackListRideRepository.cachedItems.map { it.uuid }))
            .groupBy(Cvdata.CVDATA.MINIGAME_SCORE.UUID)
            .orderBy(scoreFieldSorted.let { if (scoreAscending) it else it.desc() })
            .limit(limit)
//            .apply {
//                Logger.debug(sql)
//            }
            .fetchInto(dao.type)/*.also {
                Logger.debug("For TopCounters $gameId limit=$limit type=$type asc=$scoreAscending crew=$includeCrew $before<->$after aggr=$scoreAggregate $uuids")
                Logger.debug("Result ${it.size}:\n- ${it.joinToString("\n- ") { "${it.uuid} -> ${it.score}" }}")
            }*/
    }

    enum class ScoreAggregate {
        MIN,
        MAX
    }
}