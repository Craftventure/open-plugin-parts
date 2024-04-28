package net.craftventure.core.database.model.ownable

import com.squareup.moshi.JsonClass
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.entity.Player

@JsonClass(generateAdapter = true)
data class PredecessorRideCount(
    val name: String,
    val count: Int? = null,
    val onlyShowIfMissing: Boolean = false
) {
    fun hasCompleted(player: Player) =
        MainRepositoryProvider.rideCounterRepository.get(player.uniqueId, name)?.count?.let {
            it >= (count ?: 1)
        } ?: false
}