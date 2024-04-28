package net.craftventure.audioserver.event

import net.craftventure.audioserver.websocket.ChannelMetaData
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class AudioServerConnectedEvent(who: Player, val channelMetaData: ChannelMetaData) : PlayerEvent(who) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
