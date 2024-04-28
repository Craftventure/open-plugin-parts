package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.actions.ExitNonVipToNearestWarp
import net.craftventure.core.feature.kart.actions.ExitToNearestWarp
import net.craftventure.core.feature.kart.actions.ExitToSafeLocation

@JsonClass(generateAdapter = true)
data class KartExitAction(
    val type: String,
    val data: String? = null
) {
    fun toAction() = when (type) {
        "warp/nearest" -> ExitToNearestWarp()
        "warp/non_vip_nearest" -> ExitNonVipToNearestWarp()
        "safe" -> ExitToSafeLocation()
        else -> null
    }
}