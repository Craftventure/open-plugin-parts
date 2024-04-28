package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.MapEntryDao
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import net.craftventure.database.generated.cvdata.tables.records.MapEntryRecord
import org.jooq.Configuration

class MapEntriesRepository(
    configuration: Configuration
) : BaseIdRepository<MapEntryRecord, MapEntry, Int>(
    MapEntryDao(configuration),
    shouldCache = true
) {
    fun getByTrigger(trigger: String): List<MapEntry> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.MAP_ENTRY.UPDATE_TRIGGER.eq(trigger))
            .fetchInto(dao.type)
    }

    fun getByName(name: String): MapEntry? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.MAP_ENTRY.NAME.eq(name))
            .fetchOneInto(dao.type)
    }

    fun getByMapId(id: Int): MapEntry? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.MAP_ENTRY.MAP_ID.eq(id))
            .fetchOneInto(dao.type)
    }
}