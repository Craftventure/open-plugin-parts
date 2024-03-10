package net.craftventure.audioserver.packet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import net.craftventure.audioserver.websocket.ChannelMetaData
import net.craftventure.core.ktx.json.MoshiBase

abstract class BasePacket(
    @field:Transient
    val packetId: PacketID,
    @Json(name = "id")
    var id: Int = packetId.id
) {
    private fun toJson(): String {
        return MoshiBase.moshi.adapter<BasePacket>(this::class.java).toJson(this)
    }

    private fun requireAllowSend() {
        if (!packetId.direction.allowsSend) throw IllegalStateException("$packetId is not meant to be sent!")
    }

    fun send(channelMetaData: ChannelMetaData) {
        send(channelMetaData.channel as NioSocketChannel?)
    }

    fun send(channelHandlerContext: ChannelHandlerContext?) {
        requireAllowSend()
        if (channelHandlerContext != null && channelHandlerContext.channel().isOpen) {
            channelHandlerContext.writeAndFlush(TextWebSocketFrame(toJson()))
        }
    }

    fun send(channel: NioSocketChannel?) {
        requireAllowSend()
        if (channel != null && channel.isOpen) {
            channel.writeAndFlush(TextWebSocketFrame(toJson()))
        }
    }

    companion object {
        fun <T : BasePacket> fromJson(json: String, classOfT: Class<T>): T? {
            return MoshiBase.moshi.adapter(classOfT).fromJson(json)
        }

        fun getPacketIdFromRawJson(json: String): PacketID? {
            try {
                val packet = MoshiBase.moshi.adapter(Packet::class.java).fromJson(json)
                if (packet != null)
                    return PacketID.fromId(packet.id)
            } catch (e: Exception) {
            }
            return null
        }
    }

    @JsonClass(generateAdapter = true)
    data class Packet(val id: Int)
}
