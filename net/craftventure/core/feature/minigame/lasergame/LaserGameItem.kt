package net.craftventure.core.feature.minigame.lasergame

import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.utils.InterpolationUtils
import java.lang.Long.max

interface LaserGameItem {
    var lastUse: Long
    var cooldownFinish: Long

    fun applySwitchCooldown(cooldown: Long) {
        if (cooldownFinish < System.currentTimeMillis()) {
            lastUse = System.currentTimeMillis()
        }
        cooldownFinish = max(cooldownFinish, lastUse + cooldown)
    }

    fun getCooldownPercentageLeft(now: Long): Double {
        if (cooldownFinish < now) return 0.0
//        val percentage = (lastUse..cooldownFinish)
        return 1.0 - InterpolationUtils.getMu(lastUse.toDouble(), cooldownFinish.toDouble(), now.toDouble())
            .clamp(0.0, 1.0)
    }

    /**
     * @return true if the item was used
     */
    fun use(game: LaserGame, player: LaserGamePlayer, leftClick: Boolean): Boolean
}