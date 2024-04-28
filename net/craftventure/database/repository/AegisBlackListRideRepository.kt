package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.AegisBlacklistRidesDao
import net.craftventure.database.generated.cvdata.tables.pojos.AegisBlacklistRides
import net.craftventure.database.generated.cvdata.tables.records.AegisBlacklistRidesRecord
import org.jooq.Configuration
import java.util.*

class AegisBlackListRideRepository(
    configuration: Configuration
) : BaseIdRepository<AegisBlacklistRidesRecord, AegisBlacklistRides, UUID>(
    AegisBlacklistRidesDao(configuration),
    shouldCache = true
) {
    fun getRecord(uuid: UUID) = withDsl { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.AEGIS_BLACKLIST_RIDES.UUID.eq(uuid))
            .fetchInto(dao.type)
            .firstOrNull()
    }
}