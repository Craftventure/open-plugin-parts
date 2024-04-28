package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.WarpDao
import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import net.craftventure.database.generated.cvdata.tables.records.WarpRecord
import org.jooq.Configuration

class WarpRepository(
    configuration: Configuration
) : BaseIdRepository<WarpRecord, Warp, String>(
    WarpDao(configuration),
    shouldCache = true
) {
    fun findByName(name: String) = withDsl { dsl ->
        (dao as WarpDao).fetchOneById(name)
    }

    fun findCachedByName(name: String) = cachedItems.firstOrNull { it.id == name }

    fun deleteByNameSilent(name: String) = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.WARP.ID.eq(name))
            .execute() == 1
    }
}