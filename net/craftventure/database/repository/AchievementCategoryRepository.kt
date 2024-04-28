package net.craftventure.database.repository

import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.daos.AchievementCategoryDao
import net.craftventure.database.generated.cvdata.tables.pojos.Achievement
import net.craftventure.database.generated.cvdata.tables.pojos.AchievementCategory
import net.craftventure.database.generated.cvdata.tables.records.AchievementCategoryRecord
import org.jooq.Configuration
import java.util.*

class AchievementCategoryRepository(
    configuration: Configuration
) : BaseIdRepository<AchievementCategoryRecord, AchievementCategory, String>(
    AchievementCategoryDao(configuration),
    shouldCache = true
) {
    private var cachedCategories = hashMapOf<String, CachedAchievementCategory>()

    val cachedCategoriesList: Collection<CachedAchievementCategory>
        get() = cachedCategories.values

    override fun onAfterCache() {
        super.onAfterCache()
        val cachedCategories = hashMapOf<String, CachedAchievementCategory>()
        cachedItems.forEach { category ->
            val achievements =
                MainRepositoryProvider.achievementRepository.itemsPojo().filter { it.category == category.id }
            cachedCategories[category.id!!] = CachedAchievementCategory(category, achievements)
        }
        this.cachedCategories = cachedCategories
    }

    fun cached(id: String): CachedAchievementCategory? {
        val id = id.lowercase(Locale.getDefault())
        if (cachedCategories.containsKey(id))
            return cachedCategories[id]
        val achievementCategory = findSilent(id)
        if (achievementCategory != null) {
            val cachedShop = CachedAchievementCategory(achievementCategory,
                MainRepositoryProvider.achievementRepository.itemsPojo()
                    .filter { it.category == achievementCategory.id })
            cachedCategories[id] = cachedShop
            return cachedShop
        }
        return null
    }

    data class CachedAchievementCategory(
        val achievementCategory: AchievementCategory,
        val achievements: List<Achievement>,
    ) {
        val categoryName: String?
            get() = achievementCategory.displayName
    }
}