package net.craftventure.database.bukkit.extensions

import net.craftventure.database.generated.cvdata.tables.pojos.Achievement
import net.craftventure.database.generated.cvdata.tables.pojos.AchievementProgress
import net.craftventure.database.type.AchievementType
import org.bukkit.entity.Player


fun Achievement.isVisibleToPlayer(player: Player, relatedRewardedAchievement: AchievementProgress?): Boolean {
    require(relatedRewardedAchievement == null || relatedRewardedAchievement.achievementId == id) { "Related achievement id ${relatedRewardedAchievement?.achievementId} does not match achievement id $id" }
    if (!enabled!!)
        return false
    if (isHistoric!! && relatedRewardedAchievement == null)
        return false
    if (type === AchievementType.SECRET && relatedRewardedAchievement == null)
        return false
    return !(type === AchievementType.ROAMING_DISPLAY_IF_UNLOCKED && relatedRewardedAchievement == null)
}