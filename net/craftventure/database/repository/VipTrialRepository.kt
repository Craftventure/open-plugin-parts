package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.VipTrialDao
import net.craftventure.database.generated.cvdata.tables.pojos.VipTrial
import net.craftventure.database.generated.cvdata.tables.records.VipTrialRecord
import org.jooq.Configuration
import org.jooq.Record2
import org.jooq.impl.DSL
import java.time.LocalDateTime
import java.util.*

class VipTrialRepository(
    configuration: Configuration
) : BaseIdRepository<VipTrialRecord, VipTrial, UUID>(
    VipTrialDao(configuration)
) {
    @Throws(Exception::class)
    fun getAllAcceptedInvites(): List<VipTrial>? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.USED_AT.isNotNull)
                .query
                .fetchInto(VipTrial::class.java)
        }
    }

    @Throws(Exception::class)
    fun getAllInvites(): List<VipTrial>? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .query
                .fetchInto(VipTrial::class.java)
        }
    }

    @Throws(Exception::class)
    fun removeInvite(sender: UUID, invitee: UUID, force: Boolean = false): Int {
        return withDsl { dsl ->
            dsl.delete(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.SENDER.eq(sender))
                .and(Cvdata.CVDATA.VIP_TRIAL.INVITEE.eq(invitee))
                .let {
                    if (force)
                        it
                    else
                        it.and(Cvdata.CVDATA.VIP_TRIAL.USED_AT.isNotNull)
                }
                .execute()
        }
    }

    @Throws(Exception::class)
    fun getInvitesById(uuid: UUID): List<VipTrial>? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.SENDER.eq(uuid))
                .query
                .fetchInto(VipTrial::class.java)
        }
    }

    @Throws(Exception::class)
    fun getInvitesToId(uuid: UUID): List<VipTrial>? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.INVITEE.eq(uuid))
                .query
                .fetchInto(VipTrial::class.java)
        }
    }

    @Throws(Exception::class)
    fun getAcceptedInvites(uuid: UUID): List<VipTrial>? {
        return withDsl { dsl ->
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.INVITEE.eq(uuid))
                .and(Cvdata.CVDATA.VIP_TRIAL.USED_AT.isNotNull)
                .query
                .fetchInto(VipTrial::class.java)
        }
    }

    @Throws(Exception::class)
    fun hasInviteSlots(sender: UUID, invitee: UUID): Boolean {
        return withDsl { dsl ->
            val sendCount = dsl.selectCount()
                .from(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.SENDER.eq(sender))
                .fetch(DSL.count()).first()
            sendCount < 3
        }
    }

    @Throws(Exception::class)
    fun canInvite(sender: UUID, invitee: UUID): Boolean {
        return withDsl { dsl ->
            val sendCount = dsl.selectCount()
                .from(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.SENDER.eq(sender))
                .fetch(DSL.count()).first()
            if (sendCount < 3) {
                val inviteCount = dsl.selectCount()
                    .from(table)
                    .where(Cvdata.CVDATA.VIP_TRIAL.INVITEE.eq(invitee))
                    .fetch(DSL.count()).first()
                return@withDsl inviteCount == 0
            }
            return@withDsl false
        }
    }

    @Throws(Exception::class)
    fun invite(sender: UUID, invitee: UUID): Boolean {
        return withDsl { dsl ->
            if (!canInvite(sender, invitee)) {
                return@withDsl false
            }

            val vipTrial = dsl.newRecord(Cvdata.CVDATA.VIP_TRIAL)
//                val vipTrial = VipTrial()
            vipTrial.apply {
                this.sender = sender
                this.invitee = invitee
                this.createdAt = LocalDateTime.now()
            }

            val insert = dsl.insertInto(table)
                .set(vipTrial.intoMap())
                .onDuplicateKeyIgnore()
                .execute()
            return@withDsl insert == 1
        }
    }

    @Throws(Exception::class)
    fun deleteUnusedInvite(sender: UUID, invitee: UUID): Int {
        return withDsl { dsl ->
            dsl.delete(table)
                .where(Cvdata.CVDATA.VIP_TRIAL.SENDER.eq(sender))
                .and(Cvdata.CVDATA.VIP_TRIAL.INVITEE.eq(invitee))
                .and(Cvdata.CVDATA.VIP_TRIAL.USED_AT.isNull)
                .execute()
        }
    }

    @Throws(Exception::class)
    fun acceptInvite(vipTrial: VipTrial): Boolean {
        return acceptInvite(vipTrial.sender!!, vipTrial.invitee!!)
    }

    @Throws(Exception::class)
    fun acceptInvite(sender: UUID, invitee: UUID): Boolean {
        getAcceptedInvites(invitee)?.let {
            if (it.isNotEmpty()) {
                return false
            }
        }
        return withDsl { dsl ->
            dsl.update(table)
                .set(Cvdata.CVDATA.VIP_TRIAL.USED_AT, LocalDateTime.now())
                .where(Cvdata.CVDATA.VIP_TRIAL.SENDER.eq(sender))
                .and(Cvdata.CVDATA.VIP_TRIAL.INVITEE.eq(invitee))
                .execute() == 1
        }
    }

}