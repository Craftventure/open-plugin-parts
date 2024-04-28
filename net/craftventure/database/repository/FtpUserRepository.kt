package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.FtpUserDao
import net.craftventure.database.generated.cvdata.tables.pojos.FtpUser
import net.craftventure.database.generated.cvdata.tables.records.FtpUserRecord
import org.jooq.Configuration
import java.util.*

class FtpUserRepository(
    configuration: Configuration
) : BaseIdRepository<FtpUserRecord, FtpUser, UUID>(
    FtpUserDao(configuration)
) {
    @Throws(Exception::class)
    fun getAllUsers(): List<FtpUser> = withDsl { dsl ->
        dsl.selectFrom(table)
            .query
            .fetchInto(dao.type)
    }

    @Throws(Exception::class)
    fun deleteByMinecraftUuid(owner: UUID): Int {
        return withDsl { dsl ->
            dsl.delete(table)
                .where(Cvdata.CVDATA.FTP_USER.MINECRAFT_UUID.eq(owner))
                .execute()
        }
    }

    @Throws(Exception::class)
    fun getByMinecraftUuid(uuid: UUID): FtpUserRecord? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.FTP_USER.MINECRAFT_UUID.eq(uuid))
                .query
                .fetch()
                ?.firstOrNull()
        }
    }

    @Throws(Exception::class)
    fun create(sender: UUID, password: String): Boolean {
        return withDsl { dsl ->
            val user = dsl.newRecord(Cvdata.CVDATA.FTP_USER)
            user.apply {
                this.minecraftUuid = sender
                this.password = password
            }

            val insert = dsl.insertInto(table)
                .set(user.intoMap())
                .onDuplicateKeyIgnore()
                .execute()
            insert == 1
        }
    }
}