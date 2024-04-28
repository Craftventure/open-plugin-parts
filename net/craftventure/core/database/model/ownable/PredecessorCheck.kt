package net.craftventure.core.database.model.ownable

import com.squareup.moshi.JsonClass
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.entity.Player
import java.time.Month
import java.time.OffsetDateTime

@JsonClass(generateAdapter = true)
data class PredecessorCheck(
    val type: Type,
    val id: String? = null,
    val data: String? = null,
    val displayName: String? = null
) {
    fun displayName(): String {
        return displayName ?: when (type) {
            Type.CODED -> {
                when (id) {
                    "season" -> {
                        val season = Season.valueOf(data!!)
                        return "Season must be meteorological ${season.displayName}"
                    }
                    "owns_flying_kart" -> {
                        return "Own a flying kart"
                    }
                    else -> null
                }
            }
            else -> null
        } ?: "?"
    }

    fun hasCompleted(player: Player): Boolean {
        when (type) {
            Type.CODED -> {
                when (id) {
                    "season" -> {
                        val season = Season.valueOf(data!!)
                        val month = OffsetDateTime.now().month
                        return when (season) {
                            Season.SPRING -> month == Month.MARCH || month == Month.APRIL || month == Month.MAY
                            Season.SUMMER -> month == Month.JUNE || month == Month.JULY || month == Month.AUGUST
                            Season.FALL -> month == Month.SEPTEMBER || month == Month.OCTOBER || month == Month.NOVEMBER
                            Season.WINTER -> month == Month.DECEMBER || month == Month.JANUARY || month == Month.FEBRUARY
                        }
                    }
                    "owns_flying_kart" -> {
                        val items = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId)
                        val ids = items.map { it.ownedItemId }
                        return ids.any { it!!.startsWith("kart_hyperion_") } || "kart_parrot" in ids || "kart_bee" in ids
                    }
                }
            }
        }
        return false
    }

    enum class Type {
        CODED
    }

    enum class Season(val displayName: String) {
        SPRING("spring"),
        SUMMER("summer"),
        FALL("fall"),
        WINTER("winter")
    }
}