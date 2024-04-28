package net.craftventure.audioserver.websocket

import com.google.gson.JsonSyntaxException
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.ProtocolConfig
import net.craftventure.audioserver.event.AudioServerConnectedEvent
import net.craftventure.audioserver.packet.*
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ChannelHandler.Sharable
class AudioServerHandler : SimpleChannelInboundHandler<TextWebSocketFrame>() {

    @Throws(Exception::class)
    override fun channelRead0(channelHandlerContext: ChannelHandlerContext, textWebSocketFrame: TextWebSocketFrame) {
        try {
            val text = textWebSocketFrame.text()
            //        Logger.console(text);
            //        textWebSocketFrame.release();
            val channel = channelHandlerContext.channel()
            val channelMetaData = channelMetaDataHashMap[channel]
            if (channelMetaData != null && text != null) {
                //            Logger.console("Text " + text);
                val id = BasePacket.getPacketIdFromRawJson(text)
                if (id != null) {
                    if (!id.direction.allowsReceive) {
                        PacketKick("Illegal packet received").send(channelHandlerContext)
                        return
                    }
                    if (id == PacketID.LOGIN) {
                        val login = BasePacket.fromJson(text, PacketLogin::class.java)!!
                        val player =
                            if (login.uuid.length <= 16) Bukkit.getPlayer(login.uuid) else Bukkit.getPlayer(
                                UUID.fromString(
                                    login.uuid
                                )
                            )

                        if (player != null) {
                            if (channel.isOpen) {
                                if (login.version > ProtocolConfig.MAX_VERSION) {
                                    PacketKick("You are using a future version of the AudioServer client which is currently unsupported!").send(
                                        channelHandlerContext
                                    )
                                    channel.close()
                                } else if (login.version < ProtocolConfig.MIN_VERSION) {
                                    PacketKick("You are using an outdated AudioServer client. If you are using the web version, try a refresh using CTRL+SHIFT+R on Windows/Linux or CMD+SHIFT+R on macOS").send(
                                        channelHandlerContext
                                    )
                                    channel.close()
                                } else if (AudioServer.instance.audioServer!!.hasJoined(player)) {
                                    PacketKick("You are already connected to the AudioServer!").send(
                                        channelHandlerContext
                                    )
                                    channel.close()
                                } else if (!MainRepositoryProvider.audioServerLinkRepository.isValid(
                                        player.uniqueId,
                                        login.auth
                                    )
                                ) {
                                    PacketKick("You have to use the /audio command first to generate a personal AudioServer URL").send(
                                        channelHandlerContext
                                    )
                                    channel.close()
                                } else if (MainRepositoryProvider.audioServerLinkRepository.isValid(
                                        player.uniqueId,
                                        login.auth
                                    )
                                ) {
//                                    try {
//                                        val ip1 = player.address!!.address.hostAddress
//                                        val ip2 =
//                                            (channelHandlerContext.channel()
//                                                .remoteAddress() as InetSocketAddress).address.hostAddress
//
//                                        val same = ip1.equals(ip2, ignoreCase = true)
//                                        if (!same) {
//                                            PacketKick("Please use the same internet connection you're using for Minecraft").send(
//                                                channelHandlerContext
//                                            )
//                                            channel.close()
//                                            return
//                                        }
//                                        //                                        Logger.info("%s connected to AudioServer with IP %s and %s, same=%s", false,
//                                        //                                                player.getName(),
//                                        //                                                ip1,
//                                        //                                                ip2,
//                                        //                                                ip1.equalsIgnoreCase(ip2)
//                                        //                                        );
//                                    } catch (e: Exception) {
//                                        e.printStackTrace()
//                                    }

                                    channelMetaData.player = player
                                    channelMetaData.protocolVersion = login.version
                                    channelMetaData.authorized()
                                    PacketClientAccept(
                                        "main",
                                        player.uniqueId.toString()
                                    ).send(channelHandlerContext)

                                    val location = player.location
                                    PacketLocationUpdate(
                                        location.x, location.y, location.z,
                                        location.yaw, location.pitch
                                    ).send(channelHandlerContext)

                                    //                                    Logger.debug("Handling join for %d areas", false, AudioServer.getInstance().getAudioServerConfig().getAreas().size());
                                    for (audioArea in AudioServer.instance.audioServerConfig!!.areas) {
                                        audioArea.handleJoin(player)
                                    }

                                    channelMetaData.player!!.sendMessage(
                                        Translation.AUDIOSERVER_CONNECTED.getTranslation(
                                            channelMetaData.player
                                        )!!
                                    )

                                    Bukkit.getScheduler().scheduleSyncDelayedTask(PluginProvider.getInstance()) {
                                        Bukkit.getServer().pluginManager.callEvent(
                                            AudioServerConnectedEvent(player, channelMetaData)
                                        )
                                    }
                                } else {
                                    PacketKick("Failed to authorise correctly. Make sure you have entered the right username and authcode!").send(
                                        channelHandlerContext
                                    )
                                    channel.close()
                                }
                            }
                        } else {
                            if (channel.isOpen) {
                                PacketKick("You have to be online on Craftventure in order to use the AudioServer").send(
                                    channelHandlerContext
                                )
                                channel.close()
                            }
                        }
                    } else if (id == PacketID.PING) {
                        PacketPing(System.currentTimeMillis()).send(channelHandlerContext)
                    } else if (id == PacketID.VOLUME) {
                        val packetVolume = BasePacket.fromJson(text, PacketVolume::class.java)!!
                        if (packetVolume.volume in 0.0..1.0) {
                            val player = channelMetaData.player
                            if (player != null)
                                MessageBarManager.display(
                                    player,
                                    Component.text(
                                        (packetVolume.type
                                            ?: PacketVolume.VolumeType.master.name).capitalize() + " volume set to " + (packetVolume.volume * 100).toInt() + "%",
                                        CVTextColor.serverNotice
                                    ),
                                    MessageBarManager.Type.AUDIOSERVER_AREA,
                                    TimeUtils.secondsFromNow(1.0),
                                    ChatUtils.ID_AUDIOSERVER_AREA_NAME
                                )
                            //                        Logger.console("Changing volume to " + packetVolume.getVolume() + " for " + channelMetaData.getPlayer().getName());
                        }
                    } else if (id == PacketID.DISPLAY_AREA_ENTER) {
                        val packet = BasePacket.fromJson(text, PacketDisplayAreaEnter::class.java)!!
                        //                    Logger.console("Display area enter " + packet.getName());
                        if (packet.name != null) {
                            val audioArea = AudioServer.instance.audioServerConfig!!.getAudioAreaByName(packet.name)
                            audioArea?.sendDisplayEnterMessage(channelMetaData.player, channelMetaData)
                        } else {
                            channelMetaData.lastSentAreaTitle = null
                        }
                    } else if (id == PacketID.OPERATOR_CONTROL_CLICK) {
                        val packet = BasePacket.fromJson(text, PacketOperatorControlClick::class.java)!!
                        AudioServer.instance.operatorDelegate?.invoke(
                            channelMetaData.player!!,
                            packet.rideId,
                            packet.controlId
                        )
                        //                        Logger.console("Packet operator click %s %s", packet.getRideId(), packet.getControlId());
                    }// else {
                    //                    Logger.capture(new IllegalStateException(""));
                    //                }
                } else {
                    PacketKick("Data corruption detected. Please reload this webpage!").send(channelHandlerContext)
                    channel.disconnect()
                }
            }
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            PacketKick("Something crashed on Craftventure. Please login again or report this issue to the crew.").send(
                channelHandlerContext
            )

            try {
                val channel = channelHandlerContext.channel()
                val channelMetaData = channelMetaDataHashMap[channel]
                val player = channelMetaData?.player
                Logger.severe(
                    "%s (%s) tried to send packet %s",
                    false,
                    channel.remoteAddress(),
                    player?.name,
                    textWebSocketFrame.text()
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            channelHandlerContext.disconnect()
        }

    }

    @Throws(Exception::class)
    override fun channelActive(channelHandlerContext: ChannelHandlerContext) {
        super.channelActive(channelHandlerContext)
        //        Logger.console("AudioServer connection from " + channelHandlerContext.channel().remoteAddress());
        channelMetaDataHashMap[channelHandlerContext.channel()] = ChannelMetaData(channelHandlerContext.channel())
    }

    @Throws(Exception::class)
    override fun channelInactive(channelHandlerContext: ChannelHandlerContext) {
        super.channelInactive(channelHandlerContext)
        //        Logger.console("channelInactive" + channelHandlerContext.channel().remoteAddress());
        val channelMetaData = channelMetaDataHashMap[channelHandlerContext.channel()]
        if (channelMetaData != null) {
            val player = channelMetaData.player
            player?.sendMessage(Translation.AUDIOSERVER_DISCONNECTED.getTranslation(player)!!)
            channelMetaData.release()
            //            Logger.console("ChannelMetaData released");
        } else {
            Logger.severe("Failed to remove ChannelMetaData!", false)
        }
        channelMetaDataHashMap.remove(channelHandlerContext.channel())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

    companion object {

        private val channelMetaDataHashMap = ConcurrentHashMap<Channel, ChannelMetaData>()

        val channelMetaData: Collection<ChannelMetaData>
            get() = channelMetaDataHashMap.values

        fun getChannel(player: Player): ChannelMetaData? {
            for ((_, channelMetaData) in channelMetaDataHashMap) {
                if (player === channelMetaData.player) {
                    return channelMetaData
                }
            }
            return null
        }

        fun disconnect(player: Player, messsage: String): Boolean {
            for (channel in channelMetaDataHashMap.keys) {
                val channelMetaData = channelMetaDataHashMap[channel]

                if (channelMetaData?.player != null && channelMetaData.player === player) {
                    PacketKick(messsage).send(channel as NioSocketChannel)
                    channel.close()
                    return true
                }
            }
            return false
        }
    }
}
