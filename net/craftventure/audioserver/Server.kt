package net.craftventure.audioserver

import net.craftventure.audioserver.packet.BasePacket
import org.bukkit.entity.Player

interface Server {
    fun hasJoined(player: Player): Boolean

    fun sendPacket(player: Player, basePacket: BasePacket): Boolean

    fun disconnect(player: Player, message: String)

    fun stop()
}
