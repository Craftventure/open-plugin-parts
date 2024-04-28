package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.PlayerLocaleDao
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerLocale
import net.craftventure.database.generated.cvdata.tables.records.PlayerLocaleRecord
import net.craftventure.database.generated.cvdata.tables.references.PLAYER_LOCALE
import org.jooq.Configuration
import java.util.*

class PlayerLocaleRepository(
    configuration: Configuration
) : BaseIdRepository<PlayerLocaleRecord, PlayerLocale, UUID>(
    PlayerLocaleDao(configuration),
) {
    fun findByUuid(uuid: UUID) = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table).where(PLAYER_LOCALE.UUID.eq(uuid)).fetchOneInto(dao.type)
    }

    fun createIfNotExists(uuid: UUID, locale: Locale): PlayerLocale? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table).where(PLAYER_LOCALE.UUID.eq(uuid)).fetchOneInto(dao.type) ?: PlayerLocale(
            UUID.randomUUID(),
            uuid,
            locale.toString()
        ).let {
            if (createOrUpdateSilent(it)) it
            else null
        }
    }
}