package net.craftventure.audioserver.websocket

import io.netty.channel.Channel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.event.AudioServerDisconnectedEvent
import net.craftventure.audioserver.packet.BasePacket
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.json.toJson
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class ChannelMetaData(channel: Channel) {
    var player: Player? = null
    var protocolVersion: Int = 0
    var channel: Channel? = null
        private set
    private val sentAreas = ArrayList<Long>()
    var lastSentAreaTitle: String? = null
    private var authorizeTimeoutRunnable: BukkitRunnable? = object : BukkitRunnable() {
        override fun run() {
            if (channel.isOpen) {
                Logger.warn("AudioServer Authorization timed out for " + channel.remoteAddress())
                channel.disconnect()
            } else {
                Logger.warn("AudioServer Authorization timed out for null channel")
            }
        }
    }

    fun send(json: String) = (channel as? NioSocketChannel)?.writeAndFlush(TextWebSocketFrame(json))
    fun send(packet: BasePacket) = (channel as? NioSocketChannel)?.writeAndFlush(TextWebSocketFrame(packet.toJson()))

    fun addSentArea(areaId: Long) {
        if (!sentAreas.contains(areaId)) {
            sentAreas.add(areaId)
        }
    }

    fun hasSentArea(areaId: Long): Boolean {
        return sentAreas.contains(areaId)
    }

    init {
        this.channel = channel
        authorizeTimeoutRunnable!!.runTaskLater(PluginProvider.getInstance(), (20 * 5).toLong())
    }

    fun authorized() {
        cancelAuthorizeTask()
    }

    fun release() {
        val player = this.player
        if (player != null) {
            if (PluginProvider.getInstance().isEnabled)
                Bukkit.getScheduler().callSyncMethod(PluginProvider.getInstance()) {
                    Bukkit.getServer().pluginManager.callEvent(AudioServerDisconnectedEvent(player))
                }
            if (AudioServer.instance.audioServerConfig != null) {
                val areaParts = AudioServer.instance.audioServerConfig!!.areaParts
                for (i in areaParts.indices) {
                    val areaPart = areaParts[i]
                    areaPart.removePlayer(player)
                }
                for (audioArea in AudioServer.instance.audioServerConfig!!.areas) {
                    audioArea.removePlayer(player)
                }
            }
        }

        this.player = null
        this.channel = null
        cancelAuthorizeTask()
    }

    private fun cancelAuthorizeTask() {
        if (this.authorizeTimeoutRunnable != null) {
            this.authorizeTimeoutRunnable!!.cancel()
            this.authorizeTimeoutRunnable = null
        }
    }
}
