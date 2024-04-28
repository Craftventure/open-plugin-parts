package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.DonationPackageDao
import net.craftventure.database.generated.cvdata.tables.pojos.DonationPackage
import net.craftventure.database.generated.cvdata.tables.records.DonationPackageRecord
import org.jooq.Configuration
import org.jooq.Record2

class DonationPackageRepository(configuration: Configuration) :
    BaseIdRepository<DonationPackageRecord, DonationPackage, Record2<Int?, Int?>>(
        DonationPackageDao(configuration),
        shouldCache = true
    ) {
    fun removeAllForTransaction(transactionId: Int) =
        withDsl { dsl ->
            dsl.deleteFrom(table)
                .where(Cvdata.CVDATA.DONATION_PACKAGE.TRANSACTION_ID.eq(transactionId))
                .execute() >= 0
        }

    fun findByTransactionId(transactionId: Int) = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.DONATION_PACKAGE.TRANSACTION_ID.eq(transactionId))
            .fetchInto(dao.type)
    }
}