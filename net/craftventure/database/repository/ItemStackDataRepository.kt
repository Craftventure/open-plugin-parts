package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.ItemStackDataDao
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.records.ItemStackDataRecord
import org.jooq.Configuration

class ItemStackDataRepository(
    configuration: Configuration
) : BaseIdRepository<ItemStackDataRecord, ItemStackData, String>(
    ItemStackDataDao(configuration),
    shouldCache = true
)