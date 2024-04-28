package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.MailDao
import net.craftventure.database.generated.cvdata.tables.pojos.Mail
import net.craftventure.database.generated.cvdata.tables.records.MailRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class MailRepository(
    configuration: Configuration
) : BaseIdRepository<MailRecord, Mail, UUID>(
    MailDao(configuration),
) {
    fun deleteMail(receiverUuid: UUID, mailUuid: UUID): Int {
        try {
            return withDsl { dsl ->
                dsl.deleteFrom(table)
                    .where(Cvdata.CVDATA.MAIL.RECEIVER_UUID.eq(receiverUuid))
                    .and(Cvdata.CVDATA.MAIL.ID.eq(mailUuid))
                    .execute()
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }
        return 0
    }

    fun cleanup(): Int {
        try {
            return withDsl { dsl ->
                dsl.deleteFrom(table)
                    .where(Cvdata.CVDATA.MAIL.EXPIRES.isNotNull)
                    .and(Cvdata.CVDATA.MAIL.EXPIRES.lt(LocalDateTime.now()))
                    .execute()
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }
        return 0
    }

    fun markAsRead(mail: UUID, read: Boolean = true): Int {
        try {
            return withDsl { dsl ->
                dsl.update(table)
                    .set(Cvdata.CVDATA.MAIL.READ, read)
                    .set(Cvdata.CVDATA.MAIL.READ_AT, LocalDateTime.now())
                    .where(Cvdata.CVDATA.MAIL.READ.eq(!read))
                    .and(Cvdata.CVDATA.MAIL.ID.eq(mail))
                    .execute()
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }
        return 0
    }

    fun getMailsFor(receiver: UUID) = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.MAIL.RECEIVER_UUID.eq(receiver))
            .fetchInto(dao.type)
    }

    fun getUnreadMailsCountFor(receiver: UUID) = withDslIgnoreErrors(0) { dsl ->
        dsl.fetchCount(table, Cvdata.CVDATA.MAIL.RECEIVER_UUID.eq(receiver).and(Cvdata.CVDATA.MAIL.READ.isFalse))
    }
}