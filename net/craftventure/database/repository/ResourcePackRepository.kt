package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.ResourcePackDao
import net.craftventure.database.generated.cvdata.tables.pojos.ResourcePack
import net.craftventure.database.generated.cvdata.tables.records.ResourcePackRecord
import org.jooq.Configuration

class ResourcePackRepository(
    configuration: Configuration
) : BaseIdRepository<ResourcePackRecord, ResourcePack, String>(
    ResourcePackDao(configuration),
) {
    fun insertOrUpdate(packs: List<ResourcePack>) = withDsl {
        try {
            val model = Cvdata.CVDATA.RESOURCE_PACK
            packs.sumOf { pack ->
                it.insertInto(table, model.URL, model.CREATED_AT, model.HASH, model.CREW)
                    .values(pack.url, pack.createdAt, pack.hash, pack.crew)
                    .onDuplicateKeyUpdate()
                    .set(model.HASH, pack.hash)
                    .set(model.CREW, pack.crew)
                    .execute()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun retain(urls: List<String>) = withDsl {
        try {
            it.deleteFrom(table)
                .where(Cvdata.CVDATA.RESOURCE_PACK.URL.notIn(urls))
                .execute()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}