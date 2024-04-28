package net.craftventure.database.repository

import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.AchievementProgressDao
import net.craftventure.database.generated.cvdata.tables.pojos.AchievementProgress
import net.craftventure.database.generated.cvdata.tables.records.AchievementProgressRecord
import net.craftventure.database.type.TransactionType
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class AchievementProgressRepository(
    configuration: Configuration
) : BaseIdRepository<AchievementProgressRecord, AchievementProgress, UUID>(
    AchievementProgressDao(configuration)
) {
    fun reset(uuid: UUID) = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.UUID.eq(uuid))
            .execute()
        true
    }

    fun get(uuid: UUID, achievementId: String): AchievementProgress? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.UUID.eq(uuid))
            .and(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.ACHIEVEMENT_ID.eq(achievementId))
            .fetchOneInto(dao.type)
    }

    fun getUnlockedOnly(uuid: UUID, achievementId: String): AchievementProgress? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.UUID.eq(uuid))
            .and(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.ACHIEVEMENT_ID.eq(achievementId))
            .and(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.COUNT.gt(0))
            .fetchOneInto(dao.type)
    }

    fun findByPlayer(uuid: UUID): List<AchievementProgress> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.UUID.eq(uuid))
            .fetchInto(dao.type)
    }

    fun increaseCounter(uuid: UUID, achievementId: String): Boolean = withDslIgnoreErrors(false) { dsl ->
        val achievement = MainRepositoryProvider.achievementRepository.findSilent(achievementId)
        if (achievement != null && achievement.enabled!! && !achievement.isHistoric!!) {
            val updated = dsl.update(table)
                .set(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.COUNT, Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.COUNT.plus(1))
                .set(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.LAST_AT, LocalDateTime.now())
                .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.UUID.eq(uuid))
                .and(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.ACHIEVEMENT_ID.eq(achievementId))
                .execute() == 1
            if (updated) {
                val rewardedAchievement = get(uuid, achievementId)
                if (rewardedAchievement != null) {
                    if (rewardedAchievement.count == 1 && achievement.vcWorth!! > 0) {
                        MainRepositoryProvider.bankAccountRepository.delta(
                            uuid,
                            achievement.bankAccountType!!,
                            achievement.vcWorth!!.toLong(),
                            TransactionType.ACHIEVEMENT
                        )
                    }
                    triggerListenerUpdate(rewardedAchievement)
                }
                return@withDslIgnoreErrors true
            } else {
                reward(uuid, achievementId)
            }
        } else {
            Logger.debug("Failed to reward achievement $achievementId null=${achievement == null} enabled==${achievement?.enabled} historic=${achievement?.isHistoric}")
        }
        return@withDslIgnoreErrors false
    }

    fun reward(uuid: UUID, achievementId: String): Boolean = withDslIgnoreErrors(false) { dsl ->
        if (DEBUG)
            logcat { "Rewarding $achievementId to $uuid" }

        val achievement = MainRepositoryProvider.achievementRepository.findSilent(achievementId)
        if (achievement != null && achievement.enabled!! && !achievement.isHistoric!!) {
            val updated = dsl.update(table)
                .set(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.COUNT, 1)
                .set(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.LAST_AT, LocalDateTime.now())
                .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.UUID.eq(uuid))
                .and(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.ACHIEVEMENT_ID.eq(achievementId))
                .and(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.COUNT.eq(0))
                .execute() == 1

            if (updated) {
                if (achievement != null) {
                    // Previously reset achievement is re-rewarded
                    if (achievement.vcWorth!! > 0) {
                        MainRepositoryProvider.bankAccountRepository.delta(
                            uuid,
                            achievement.bankAccountType!!,
                            achievement.vcWorth!!.toLong(),
                            TransactionType.ACHIEVEMENT
                        )
                    }

                    val rewardedAchievement = get(uuid, achievementId)
                    if (rewardedAchievement != null) {
                        if (rewardedAchievement.count == 1 && achievement.vcWorth!! > 0) {
                            MainRepositoryProvider.bankAccountRepository.delta(
                                uuid,
                                achievement.bankAccountType!!,
                                achievement.vcWorth!!.toLong(),
                                TransactionType.ACHIEVEMENT
                            )
                        }
                        triggerListenerUpdate(rewardedAchievement)
                    }
                }
                return@withDslIgnoreErrors true
            }

            if (dsl.selectFrom(table)
                    .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.UUID.eq(uuid))
                    .and(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.ACHIEVEMENT_ID.eq(achievementId))
                    .execute() == 0
            ) {
                val progress = AchievementProgress(
                    id = UUID.randomUUID(),
                    uuid = uuid,
                    achievementId = achievementId,
                    count = 1,
                    unlockedAt = LocalDateTime.now(),
                    lastAt = LocalDateTime.now()
                )
                if (createSilent(progress)) {
                    if (achievement.vcWorth!! > 0) {
                        MainRepositoryProvider.bankAccountRepository.delta(
                            uuid,
                            achievement.bankAccountType!!,
                            achievement.vcWorth!!.toLong(),
                            TransactionType.ACHIEVEMENT
                        )
                    }
                    return@withDslIgnoreErrors true
                }
            } else {

            }
        } else {
            Logger.debug("Failed to reward achievement $achievementId null=${achievement == null} enabled==${achievement?.enabled} historic=${achievement?.isHistoric}")
        }
        return@withDslIgnoreErrors false
    }

    fun getTopCounters(achievementId: String, limit: Long = 10): List<AchievementProgress> = withDslIgnoreErrors(
        emptyList()
    ) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.ACHIEVEMENT_ID.eq(achievementId))
            .orderBy(Cvdata.CVDATA.ACHIEVEMENT_PROGRESS.COUNT.desc())
            .limit(limit)
            .fetchInto(dao.type)
    }

    companion object {
        var DEBUG = true
    }
}