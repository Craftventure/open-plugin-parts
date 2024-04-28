package net.craftventure.core.feature.minigame.jumpchallenge

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import org.bukkit.entity.Player

class JumpChallengePlayer(
    val player: Player
) {
    var finishTime = -1L
        private set

    fun finished() = finishTime != -1L

    fun update(level: JumpChallengeLevel) {
        if (!finished()) {
            if (level.finish.isInArea(player)) {
                finishTime = System.currentTimeMillis()
                player.sendMessage(CVTextColor.serverNotice + "You finished! You'll have to wait for all players to finish or the time to run out though! Feel free to roam around until then!")
            }
        }
    }
}
