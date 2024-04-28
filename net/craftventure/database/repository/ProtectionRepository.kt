package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.ProtectionDao
import net.craftventure.database.generated.cvdata.tables.pojos.Protection
import net.craftventure.database.generated.cvdata.tables.records.ProtectionRecord
import org.jooq.Configuration
import java.util.*

class ProtectionRepository(
    configuration: Configuration
) : BaseIdRepository<ProtectionRecord, Protection, UUID>(
    ProtectionDao(configuration),
    shouldCache = true
) {
    fun getByPosition(world: String, x: Int, y: Int, z: Int): Protection? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.PROTECTION.WORLD.eq(world))
            .and(Cvdata.CVDATA.PROTECTION.X.eq(x))
            .and(Cvdata.CVDATA.PROTECTION.Y.eq(y))
            .and(Cvdata.CVDATA.PROTECTION.Z.eq(z))
            .fetchOneInto(dao.type)
    }

    fun deleteByPosition(world: String, x: Int, y: Int, z: Int): Boolean = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.PROTECTION.WORLD.eq(world))
            .and(Cvdata.CVDATA.PROTECTION.X.eq(x))
            .and(Cvdata.CVDATA.PROTECTION.Y.eq(y))
            .and(Cvdata.CVDATA.PROTECTION.Z.eq(z))
            .execute() == 1
    }
}