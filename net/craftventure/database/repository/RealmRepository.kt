package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.RealmDao
import net.craftventure.database.generated.cvdata.tables.pojos.Realm
import net.craftventure.database.generated.cvdata.tables.records.RealmRecord
import org.jooq.Configuration

class RealmRepository(configuration: Configuration) : BaseIdRepository<RealmRecord, Realm, String>(
    RealmDao(configuration),
    shouldCache = true
)