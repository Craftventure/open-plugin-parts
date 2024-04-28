package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.ShopDao
import net.craftventure.database.generated.cvdata.tables.pojos.Shop
import net.craftventure.database.generated.cvdata.tables.records.ShopRecord
import org.jooq.Configuration

class ShopRepository(
    configuration: Configuration
) : BaseIdRepository<ShopRecord, Shop, String>(
    ShopDao(configuration),
    shouldCache = true
)