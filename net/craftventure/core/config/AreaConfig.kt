package net.craftventure.core.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.SimpleArea
import java.time.OffsetDateTime

@JsonClass(generateAdapter = true)
class AreaConfig(
    @Json(name = "x_min")
    val xMin: Double = 0.0,

    @Json(name = "y_min")
    val yMin: Double = 0.0,

    @Json(name = "z_min")
    val zMin: Double = 0.0,

    @Json(name = "x_max")
    val xMax: Double = 0.0,

    @Json(name = "y_max")
    val yMax: Double = 0.0,

    @Json(name = "z_max")
    val zMax: Double = 0.0,

    @Json(name = "world")
    val world: String = "world",

    val enabled: Boolean = false,

    @Json(name = "block_flying")
    val blockFlying: Boolean = false,

    @Json(name = "linked_achievement")
    val linkedAchievement: String? = null,

    var name: String? = null,

    @Json(name = "coin_add")
    val coinAdd: Int = 0,

    @Json(name = "coin_multiply")
    val coinMultiply: Int = 0,

    @Json(name = "join_message")
    val joinMessage: String? = null,

    @Json(name = "leave_message")
    val leaveMessage: String? = null,

    @Json(name = "start_time")
    val startTime: OffsetDateTime? = null,

    @Json(name = "end_time")
    val endTime: OffsetDateTime? = null,

    @Json(name = "enter_flag")
    val enterFlag: String? = null,

    @Json(name = "display_name")
    val displayName: String? = null,

    @Json(name = "description")
    val description: String? = null,

    @Json(name = "reward")
    val reward: String? = null,

    @Json(name = "karting_blocked")
    val kartingBlocked: Boolean = false,

    @Json(name = "force_vanish")
    val forceVanish: Boolean = false,

    @Json(name = "run_blocked")
    val runBlocked: Boolean = false,

    @Json(name = "swim_blocked")
    val swimBlocked: Boolean = false,

    val enterWarp: String? = null,
    val forceWarp: Boolean = false,

    val requiredItems: Set<String>? = null,
    val warpForRequiredItems: String? = null,
    val requiredItemsMissingMessage: String? = null,
    val requiredItemWarpMessage: String? = null,
    val requiredItemMissingEffect: Boolean = true,
    val itemConsumptionBlocked: Boolean = false,
) {

    fun shouldCreateTracker(): Boolean {
        return enabled && (blockFlying ||
                linkedAchievement != null ||
                joinMessage != null ||
                enterFlag != null ||
                leaveMessage != null ||
                forceVanish ||
                kartingBlocked ||
                runBlocked ||
                swimBlocked ||
                reward != null ||
                (requiredItems != null && warpForRequiredItems != null))
    }

    val isWithinTimeRange: Boolean
        get() {
            if (startTime != null && System.currentTimeMillis() < startTime.toEpochSecond() * 1000) return false
            return !(endTime != null && System.currentTimeMillis() > endTime.toEpochSecond() * 1000)
        }

    val isActive: Boolean
        get() = enabled && isWithinTimeRange

    val area: SimpleArea by lazy { SimpleArea(world, xMin, yMin, zMin, xMax, yMax, zMax) }

}