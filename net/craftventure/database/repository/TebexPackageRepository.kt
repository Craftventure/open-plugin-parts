package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.TebexPackageDao
import net.craftventure.database.generated.cvdata.tables.pojos.TebexPackage
import net.craftventure.database.generated.cvdata.tables.records.TebexPackageRecord
import org.jooq.Configuration

class TebexPackageRepository(configuration: Configuration) : BaseIdRepository<TebexPackageRecord, TebexPackage, Int>(
    TebexPackageDao(configuration),
    shouldCache = true
)