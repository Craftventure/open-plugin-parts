package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.RideLinkDao
import net.craftventure.database.generated.cvdata.tables.pojos.RideLink
import net.craftventure.database.generated.cvdata.tables.records.RideLinkRecord
import org.jooq.Configuration
import java.util.*

class RideLinkRepository(
    configuration: Configuration
) : BaseIdRepository<RideLinkRecord, RideLink, UUID>(
    RideLinkDao(configuration),
    shouldCache = true
) {
    fun get(rideId: UUID): List<RideLink> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.RIDE_LINK.RIDE.eq(rideId))
            .fetchInto(dao.type)
    }
}