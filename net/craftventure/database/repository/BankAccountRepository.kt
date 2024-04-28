package net.craftventure.database.repository

import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.BankAccountDao
import net.craftventure.database.generated.cvdata.tables.pojos.BankAccount
import net.craftventure.database.generated.cvdata.tables.pojos.TransactionLog
import net.craftventure.database.generated.cvdata.tables.records.BankAccountRecord
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class BankAccountRepository(
    configuration: Configuration
) : BaseIdRepository<BankAccountRecord, BankAccount, UUID>(
    BankAccountDao(configuration),
) {
    fun set(
        uuid: UUID,
        bankAccountType: BankAccountType,
        value: Long,
        transactionType: TransactionType?
    ): Boolean = withDslIgnoreErrors(false) { dsl ->
        if (transactionType == null)
            return@withDslIgnoreErrors false
        val success = dsl.update(table)
            .set(Cvdata.CVDATA.BANK_ACCOUNT.BALANCE, value)
            .where(Cvdata.CVDATA.BANK_ACCOUNT.UUID.eq(uuid))
            .and(Cvdata.CVDATA.BANK_ACCOUNT.TYPE.eq(bankAccountType))
            .execute() == 1

        if (success) {
            val account = dsl.selectFrom(table)
                .where(Cvdata.CVDATA.BANK_ACCOUNT.UUID.eq(uuid))
                .and(Cvdata.CVDATA.BANK_ACCOUNT.TYPE.eq(bankAccountType))
                .fetchOneInto(dao.type)
            if (account != null)
                triggerListenerUpdate(account)
        }

        if (success && transactionType != TransactionType.ACTIVE_ONLINE_REWARD) {
            MainRepositoryProvider.transactionLogRepository.createSilent(
                TransactionLog(
                    id = UUID.randomUUID(),
                    uuid = uuid,
                    date = LocalDateTime.now(),
                    type = transactionType,
                    account = bankAccountType.internalName,
                    newBalance = value.toInt(),
                    delta = 0
                )
            )
        }

        success
    }


    fun delta(uuid: UUID, bankAccountType: BankAccountType, delta: Long, transactionType: TransactionType): Boolean {
        if (delta == 0L) {
            return true
        }
        return withDslIgnoreErrors(false) { dsl ->
            val success = dsl.update(table)
                .set(Cvdata.CVDATA.BANK_ACCOUNT.BALANCE, Cvdata.CVDATA.BANK_ACCOUNT.BALANCE.plus(delta))
                .where(Cvdata.CVDATA.BANK_ACCOUNT.UUID.eq(uuid))
                .and(Cvdata.CVDATA.BANK_ACCOUNT.TYPE.eq(bankAccountType))
                .execute() == 1

            if (success) {
                val new = get(uuid, bankAccountType)
                if (new != null) {
                    triggerListenerUpdate(new)
                }
            }

            if (success && transactionType != TransactionType.ACTIVE_ONLINE_REWARD) {
                MainRepositoryProvider.transactionLogRepository.createSilent(
                    TransactionLog(
                        id = UUID.randomUUID(),
                        uuid = uuid,
                        date = LocalDateTime.now(),
                        type = transactionType,
                        account = bankAccountType.internalName,
                        newBalance = delta.toInt(),
                        delta = 0
                    )
                )
            }

            success
        }
    }

    fun get(uuid: UUID, bankAccountType: BankAccountType): BankAccount? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.BANK_ACCOUNT.UUID.eq(uuid))
            .and(Cvdata.CVDATA.BANK_ACCOUNT.TYPE.eq(bankAccountType))
            .fetchOneInto(dao.type)
    }

    fun getOrCreate(uuid: UUID, bankAccountType: BankAccountType): BankAccount? = withDslIgnoreErrors(null) { dsl ->
        var bankAccount = dsl.selectFrom(table)
            .where(Cvdata.CVDATA.BANK_ACCOUNT.UUID.eq(uuid))
            .and(Cvdata.CVDATA.BANK_ACCOUNT.TYPE.eq(bankAccountType))
            .fetchOneInto(dao.type)

        if (bankAccount == null) {
            bankAccount = BankAccount(
                id = UUID.randomUUID(),
                uuid = uuid,
                type = bankAccountType,
                balance = bankAccountType.startValue
            )
            val created = createSilent(bankAccount)
            if (!created) return@withDslIgnoreErrors null
            triggerListenerCreate(bankAccount)
        }

        bankAccount
    }

    fun findAllForPlayer(uuid: UUID): List<BankAccount> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.BANK_ACCOUNT.UUID.eq(uuid))
            .fetchInto(dao.type)
    }
}