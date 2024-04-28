package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.CoinBoosterDao
import net.craftventure.database.generated.cvdata.tables.pojos.CoinBooster
import net.craftventure.database.generated.cvdata.tables.records.CoinBoosterRecord
import org.jooq.Configuration

class CoinBoosterRepository(
    configuration: Configuration
) : BaseIdRepository<CoinBoosterRecord, CoinBooster, String>(
    CoinBoosterDao(configuration),
    shouldCache = true
)