package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class AchievementCategoryType(
    val isHideIfNoAchievementsUnlocked: Boolean = false
) {
    DEFAULT,
    SECRET(true);

    companion object {
        class Converter :
            EnumConverter<String, AchievementCategoryType>(String::class.java, AchievementCategoryType::class.java)
    }
}
