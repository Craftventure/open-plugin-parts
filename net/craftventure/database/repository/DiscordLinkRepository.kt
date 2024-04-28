package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.DiscordLinkDao
import net.craftventure.database.generated.cvdata.tables.pojos.DiscordLink
import net.craftventure.database.generated.cvdata.tables.records.DiscordLinkRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class DiscordLinkRepository(
    configuration: Configuration
) : BaseIdRepository<DiscordLinkRecord, DiscordLink, UUID>(
    DiscordLinkDao(configuration)
) {
    @Throws(Exception::class)
    fun getAllActiveLinks(): List<DiscordLink> {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.DISCORD_LINK.MINECRAFT_UUID.isNotNull)
                .and(Cvdata.CVDATA.DISCORD_LINK.DISCORD_ID.isNotNull)
                .query
                .fetchInto(DiscordLink::class.java)
        }
    }

    @Throws(Exception::class)
    fun deleteByMinecraftUuid(owner: UUID): Int {
        return withDsl { dsl ->
            dsl.delete(table)
                .where(Cvdata.CVDATA.DISCORD_LINK.MINECRAFT_UUID.eq(owner))
                .execute()
        }
    }

    @Throws(Exception::class)
    fun getByMinecraftUuid(uuid: UUID): DiscordLink? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.DISCORD_LINK.MINECRAFT_UUID.eq(uuid))
                .query
                .fetchInto(DiscordLink::class.java)?.firstOrNull()
        }
    }

    @Throws(Exception::class)
    fun getByDiscordId(discordId: Long): DiscordLink? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.DISCORD_LINK.DISCORD_ID.eq(discordId))
                .query
                .fetchInto(DiscordLink::class.java)?.firstOrNull()
        }
    }

    @Throws(Exception::class)
    fun create(sender: UUID, code: String): Boolean {
        return withDsl { dsl ->
            val discordLink = dsl.newRecord(Cvdata.CVDATA.DISCORD_LINK)
            discordLink.apply {
                this.minecraftUuid = sender
                this.key = code
                this.createdAt = LocalDateTime.now()
            }

            val insert = dsl.insertInto(table)
                .set(discordLink.intoMap())
                .onDuplicateKeyIgnore()
                .execute()
            insert == 1
        }
    }

    @Throws(Exception::class)
    fun updateDiscordId(discordId: Long, key: String): Boolean {
        return withDsl { dsl ->
            dsl.update(table)
                .set(Cvdata.CVDATA.DISCORD_LINK.DISCORD_ID, discordId)
                .where(Cvdata.CVDATA.DISCORD_LINK.KEY.eq(key))
                .and(Cvdata.CVDATA.DISCORD_LINK.DISCORD_ID.isNull)
                .execute() == 1
        }
    }

    @Throws(Exception::class)
    fun updateNitroBoosting(discordId: Long, boosting: Boolean): Boolean {
        return withDsl { dsl ->
            if (boosting)
                dsl.update(table)
                    .set(Cvdata.CVDATA.DISCORD_LINK.NITRO_BOOSTING, LocalDateTime.now())
                    .where(Cvdata.CVDATA.DISCORD_LINK.DISCORD_ID.eq(discordId))
                    .and(Cvdata.CVDATA.DISCORD_LINK.NITRO_BOOSTING.isNull)
                    .execute() == 1
            else
                dsl.update(table)
                    .set(Cvdata.CVDATA.DISCORD_LINK.NITRO_BOOSTING, if (boosting) LocalDateTime.now() else null)
                    .where(Cvdata.CVDATA.DISCORD_LINK.DISCORD_ID.eq(discordId))
                    .execute() == 1
        }
    }
}