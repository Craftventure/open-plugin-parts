package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.UuidNameDao
import net.craftventure.database.generated.cvdata.tables.pojos.UuidName
import net.craftventure.database.generated.cvdata.tables.records.UuidNameRecord
import org.jooq.Configuration
import org.jooq.Record2
import java.util.*

class UuidNameRepository(
    configuration: Configuration
) : BaseIdRepository<UuidNameRecord, UuidName, Record2<UUID?, String?>>(
    UuidNameDao(configuration),
) {
    fun findAllForUuid(uuid: UUID): List<UuidName> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.UUID_NAME.UUID.eq(uuid))
            .fetchInto(UuidName::class.java)
    }
}