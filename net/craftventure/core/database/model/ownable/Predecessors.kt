package net.craftventure.core.database.model.ownable

import com.squareup.moshi.JsonClass
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
data class Predecessors(
    val items: Array<PredecessorItem> = emptyArray(),
    val checks: Array<PredecessorCheck> = emptyArray(),
    val achievements: Array<PredecessorAchievement> = emptyArray(),
    val rideCounts: Array<PredecessorRideCount> = emptyArray(),
    val hideUntilUnlocked: Boolean = false,
    val showPlaceholderWhenHidden: Boolean = false
) {
    fun hasCompleted(player: Player) =
        items.all { it.hasCompleted(player) } &&
                checks.all { it.hasCompleted(player) } &&
                achievements.all { it.hasCompleted(player) == PredecessorAchievement.Result.Yes } &&
                rideCounts.all {
                    it.hasCompleted(player)
                }

    fun hasRequirements() =
        items.isNotEmpty() || checks.isNotEmpty() || achievements.isNotEmpty() || rideCounts.isNotEmpty()

    fun shouldBeVisibleTo(player: Player) = !hideUntilUnlocked || hasCompleted(player)
    fun shouldDisplayPlaceholderTo(player: Player) = hideUntilUnlocked && showPlaceholderWhenHidden
}