package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.ConfigDao
import net.craftventure.database.generated.cvdata.tables.pojos.Config
import net.craftventure.database.generated.cvdata.tables.records.ConfigRecord
import org.jooq.Configuration

class ConfigRepository(configuration: Configuration) : BaseIdRepository<ConfigRecord, Config, String>(
    ConfigDao(configuration),
    shouldCache = true
)