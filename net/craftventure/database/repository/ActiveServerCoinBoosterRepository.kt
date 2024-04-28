package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.extension.execute
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.ActiveServerCoinBoosterDao
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveServerCoinBooster
import net.craftventure.database.generated.cvdata.tables.records.ActiveServerCoinBoosterRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class ActiveServerCoinBoosterRepository(
    configuration: Configuration
) : BaseIdRepository<ActiveServerCoinBoosterRecord, ActiveServerCoinBooster, UUID>(
    ActiveServerCoinBoosterDao(configuration),
) {
    fun update(timeSpan: Long) {
        withDslIgnoreErrors(null) { dsl ->
            dsl.update(table)
                .set(
                    Cvdata.CVDATA.ACTIVE_SERVER_COIN_BOOSTER.TIME_LEFT,
                    Cvdata.CVDATA.ACTIVE_SERVER_COIN_BOOSTER.TIME_LEFT.minus(timeSpan)
                )
                .where(Cvdata.CVDATA.ACTIVE_SERVER_COIN_BOOSTER.TIME_LEFT.ge(0))
                .execute { updateCount ->
                    if (updateCount != 0)
                        Logger.info("Updated $updateCount serverwide coin boosters", logToCrew = false)
                }
        }

        withDsl { dsl ->
            dsl.deleteFrom(table)
                .where(Cvdata.CVDATA.ACTIVE_SERVER_COIN_BOOSTER.TIME_LEFT.lt(0))
                .execute { deleted ->
                    Logger.info("Deleted $deleted expired serverwide coin boosters", logToCrew = false)
                }
        }
    }

    fun activate(uuid: UUID, boosterId: String): Boolean = withDslIgnoreErrors(false) { dsl ->
        val booster = MainRepositoryProvider.coinBoosterRepository.findCached(boosterId)
        val ownableItem = MainRepositoryProvider.coinBoosterRepository.findCached(boosterId)


        if (booster != null && ownableItem != null) {
            val duration = booster.duration
            val activeCoinBooster =
                ActiveServerCoinBooster(
                    UUID.randomUUID(),
                    booster.id,
                    uuid,
                    LocalDateTime.now(),
                    duration
                )
            return@withDslIgnoreErrors create(activeCoinBooster)
        } else {
            false
        }
    }
}