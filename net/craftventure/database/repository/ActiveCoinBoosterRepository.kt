package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.extension.execute
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.ActiveCoinBoosterDao
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveCoinBooster
import net.craftventure.database.generated.cvdata.tables.pojos.CoinBooster
import net.craftventure.database.generated.cvdata.tables.records.ActiveCoinBoosterRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class ActiveCoinBoosterRepository(
    configuration: Configuration
) : BaseIdRepository<ActiveCoinBoosterRecord, ActiveCoinBooster, UUID>(
    ActiveCoinBoosterDao(configuration),
) {
    fun reset(uuid: UUID) = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.ACTIVE_COIN_BOOSTER.UUID.eq(uuid))
            .execute()
        true
    }

    fun cleanup() = withDslIgnoreErrors(0) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.ACTIVE_COIN_BOOSTER.UNTIL.le(LocalDateTime.now()))
            .execute {
                Logger.info("Deleted $it expired coin boosters", logToCrew = false)
            }
    }

    fun getByPlayer(uuid: UUID): List<ActiveCoinBooster> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.ACTIVE_COIN_BOOSTER.UUID.eq(uuid))
            .fetchInto(dao.type)
    }

    fun activate(uuid: UUID, coinBooster: CoinBooster): Boolean {
        val activeCoinBooster =
            ActiveCoinBooster(
                UUID.randomUUID(),
                coinBooster.id,
                uuid,
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(TimeUnit.MILLISECONDS.toSeconds(coinBooster.duration!!.toLong()))
            )
        return create(activeCoinBooster)
    }
}