package net.craftventure.database.repository

import net.craftventure.database.generated.luckyperms.Luckyperms
import net.craftventure.database.generated.luckyperms.tables.records.LuckpermsUserPermissionsRecord
import org.jooq.Configuration
import org.jooq.SQLDialect
import java.util.*

class LuckPermsRepository(
    configuration: Configuration,
    sqlDialect: SQLDialect
) : BaseRepository<LuckpermsUserPermissionsRecord>(
    configuration,
    Luckyperms.LUCKYPERMS.LUCKPERMS_USER_PERMISSIONS,
    sqlDialect
) {
    @Throws(Exception::class)
    fun getGroups(player: UUID): List<String> {
        return withDsl { dsl ->
            return@withDsl dsl.selectFrom(table)
                .where(Luckyperms.LUCKYPERMS.LUCKPERMS_USER_PERMISSIONS.UUID.eq(player))
                .and(Luckyperms.LUCKYPERMS.LUCKPERMS_USER_PERMISSIONS.PERMISSION.startsWith("group."))
                .query
                .fetchInto(LuckpermsUserPermissionsRecord::class.java)
                .map { it.permission!!.split("group.")[1] }
        }
    }
}