package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.CachedGameProfileDao
import net.craftventure.database.generated.cvdata.tables.pojos.CachedGameProfile
import net.craftventure.database.generated.cvdata.tables.records.CachedGameProfileRecord
import org.jooq.Configuration

class CachedGameProfileRepository(
    configuration: Configuration
) : BaseIdRepository<CachedGameProfileRecord, CachedGameProfile, String>(
    CachedGameProfileDao(configuration),
    shouldCache = true
)