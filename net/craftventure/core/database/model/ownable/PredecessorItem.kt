package net.craftventure.core.database.model.ownable

import com.squareup.moshi.JsonClass
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
data class PredecessorItem(
    val name: String,
    val onlyShowIfMissing: Boolean = false
) {
    fun hasCompleted(player: Player) = MainRepositoryProvider.playerOwnedItemRepository.owns(player.uniqueId, name)
}