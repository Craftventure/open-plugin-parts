package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.StoreDao
import net.craftventure.database.generated.cvdata.tables.pojos.Store
import net.craftventure.database.generated.cvdata.tables.records.StoreRecord
import org.jooq.Configuration

class StoreRepository(
    configuration: Configuration
) : BaseIdRepository<StoreRecord, Store, String>(
    StoreDao(configuration),
    shouldCache = true
)