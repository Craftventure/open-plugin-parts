package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.OwnableItemDao
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.records.OwnableItemRecord
import org.jooq.Configuration

class OwnableItemRepository(configuration: Configuration) : BaseIdRepository<OwnableItemRecord, OwnableItem, String>(
    OwnableItemDao(configuration),
    shouldCache = true
)