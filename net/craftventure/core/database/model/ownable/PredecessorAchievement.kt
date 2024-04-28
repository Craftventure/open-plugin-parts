package net.craftventure.core.database.model.ownable

import com.squareup.moshi.JsonClass
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
data class PredecessorAchievement(
    val name: String,
    val overrideName: String?,
    val count: Int? = null,
    val onlyShowIfMissing: Boolean = false,
    val isPrefix: Boolean = false,
    val forceShow: Boolean = false,
) {
    fun hasCompleted(player: Player): Result {
        if (isPrefix) {
            val achievements =
                MainRepositoryProvider.achievementRepository.cachedItems.filter { it.id!!.startsWith(prefix = name) }

            var hasUnlocked = true
            var hasPartially = false
            achievements.forEach {
                val rewardedAchievement =
                    MainRepositoryProvider.achievementProgressRepository.getUnlockedOnly(player.uniqueId, it.id!!)
                        ?: return Result.No
                if (count != null) {
                    if (count > 0) hasPartially = true
                    if (rewardedAchievement.count!! < count)
                        hasUnlocked = false
                }
            }
            return if (hasUnlocked) Result.Yes else if (hasPartially) return Result.Partially else Result.No
        }
        val rewardedAchievement =
            MainRepositoryProvider.achievementProgressRepository.getUnlockedOnly(player.uniqueId, name)
                ?: return Result.No
        if (count != null)
            return if (rewardedAchievement.count!! >= count) Result.Yes else if (count > 0) Result.Partially else Result.No
        return Result.Yes
    }

    enum class Result {
        Yes,
        Partially,
        No,
    }
}