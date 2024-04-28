package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.TitleDao
import net.craftventure.database.generated.cvdata.tables.pojos.Title
import net.craftventure.database.generated.cvdata.tables.records.TitleRecord
import org.jooq.Configuration

class TitleRepository(
    configuration: Configuration
) : BaseIdRepository<TitleRecord, Title, String>(
    TitleDao(configuration),
    shouldCache = true
)