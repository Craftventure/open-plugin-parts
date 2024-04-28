package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.PlayerTimezoneDao
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerTimezone
import net.craftventure.database.generated.cvdata.tables.records.PlayerTimezoneRecord
import net.craftventure.database.generated.cvdata.tables.references.PLAYER_TIMEZONE
import org.jooq.Configuration
import java.time.ZoneId
import java.util.*

class PlayerTimezoneRepository(
    configuration: Configuration
) : BaseIdRepository<PlayerTimezoneRecord, PlayerTimezone, UUID>(
    PlayerTimezoneDao(configuration),
) {
    fun findByUuid(uuid: UUID) = withDslIgnoreErrors(null){dsl->
        dsl.selectFrom(table).where(PLAYER_TIMEZONE.UUID.eq(uuid)).fetchOneInto(dao.type)
    }

    fun createIfNotExists(uuid: UUID, zone: ZoneId): PlayerTimezone? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table).where(PLAYER_TIMEZONE.UUID.eq(uuid)).fetchOneInto(dao.type) ?: PlayerTimezone(
            UUID.randomUUID(),
            uuid,
            zone.toString()
        ).let {
            if (createOrUpdateSilent(it)) it
            else null
        }
    }
}