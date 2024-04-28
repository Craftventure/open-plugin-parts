package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.ServerDao
import net.craftventure.database.generated.cvdata.tables.pojos.Server
import net.craftventure.database.generated.cvdata.tables.records.ServerRecord
import org.jooq.Configuration

class ServerRepository(
    configuration: Configuration
) : BaseIdRepository<ServerRecord, Server, String>(
    ServerDao(configuration),
    shouldCache = true
)