package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.extension.sendPacket
import net.craftventure.core.utils.GameTimeUtils
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import org.bukkit.entity.Player
import kotlin.math.abs

object PlayerTimeManager {
    fun setFrozenTime(player: Player, hour: Int, minute: Int) =
        setFrozenTime(player, GameTimeUtils.hoursMinutesToTicks(hour, minute))

    fun setFrozenTime(player: Player, time: Long) {
//        Logger.info("Updating time for ${player.name} (frozen)")
        if (player.isPlayerTimeRelative || player.playerTime != time) {
//            Logger.info("Updating time for ${player.name}")
            player.setPlayerTime(-(player.world.fullTime - (player.world.fullTime % 24000)) - abs(time), false)
            player.sendPacket(
                ClientboundSetTimePacket(
                    player.world.time,
                    player.playerTime,
                    false,
                )
            )
        }
    }

    fun reset(player: Player) {
//        Logger.info("Resetting player time for ${player.name}")
        player.resetPlayerTime()
    }

    fun getBestTransition(fromInput: Long, toInput: Long): LongRange {
        var from = abs(fromInput) % 24000
        var to = abs(toInput) % 24000

        val delta = to - from
        if (delta > 12000) {
            to -= 24000
        } else if (delta < -12000) {
            to += 24000
        }
        while (to < 0 || from < 0) {
            to += 24000
            from += 24000
        }
        return from..to
    }
}