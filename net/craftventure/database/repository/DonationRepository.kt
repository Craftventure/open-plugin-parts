package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.DonationDao
import net.craftventure.database.generated.cvdata.tables.pojos.Donation
import net.craftventure.database.generated.cvdata.tables.records.DonationRecord
import org.jooq.Configuration
import java.util.*

class DonationRepository(configuration: Configuration) : BaseIdRepository<DonationRecord, Donation, Int>(
    DonationDao(configuration),
    shouldCache = true
) {
    fun findByPlayer(uuid: UUID) = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.DONATION.UUID.eq(uuid))
            .fetchInto(dao.type)
    }
}