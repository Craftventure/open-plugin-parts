package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.AchievementDao
import net.craftventure.database.generated.cvdata.tables.pojos.Achievement
import net.craftventure.database.generated.cvdata.tables.records.AchievementRecord
import org.jooq.Configuration

class AchievementRepository(
    configuration: Configuration
) : BaseIdRepository<AchievementRecord, Achievement, String>(
    AchievementDao(configuration),
    shouldCache = true
)