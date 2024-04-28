package net.craftventure.audioserver

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.ssl.OptionalSslHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import net.craftventure.audioserver.packet.BasePacket
import net.craftventure.audioserver.websocket.AudioServerHandler
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import org.bukkit.entity.Player
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files

class NettyServer(port: Int) : Server {
    private val serverThread: Thread

    private var sb: ServerBootstrap? = null
    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null
    private var ch: Channel? = null
    private val audioServerHandler: AudioServerHandler = AudioServerHandler()

    private val sslContext: SslContext?
        get() {
            if (!Files.isReadable(File(PluginProvider.getInstance().dataFolder, "data/audio/usessl").toPath()))
                return null

            try {
                var fullChain = File("/etc/letsencrypt/live/audiotunnel.craftventure.net/fullchain.pem")
                if (!Files.isReadable(fullChain.toPath()))
                    fullChain = File(PluginProvider.getInstance().dataFolder, "audio/fullchain.pem")

                var privKey = File("/etc/letsencrypt/live/audiotunnel.craftventure.net/privkey.pem")
                if (!Files.isReadable(privKey.toPath()))
                    privKey = File(PluginProvider.getInstance().dataFolder, "audio/privkey.pem")

                val sslContextBuilder = SslContextBuilder.forServer(fullChain, privKey)
                return sslContextBuilder.build()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }

    init {
        sb = ServerBootstrap()
        bossGroup = NioEventLoopGroup()
        workerGroup = NioEventLoopGroup()
        serverThread = Thread({
            try {
                //                    SelfSignedCertificate ssc = new SelfSignedCertificate();

                val sb = ServerBootstrap()

                sb.group(bossGroup!!, workerGroup!!)
                    .channel(NioServerSocketChannel::class.java)
                    //                            .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        @Throws(Exception::class)
                        public override fun initChannel(ch: SocketChannel) {
                            val sslContext = sslContext
                            val handlers = ArrayList<ChannelHandler>(6)
                            if (sslContext != null) {
                                handlers.add(OptionalSslHandler(sslContext))
                                //                                    handlers.add(sslContext.newHandler(ch.alloc()));
                            }
                            handlers.add(HttpRequestDecoder())
                            handlers.add(HttpObjectAggregator(65536))
                            handlers.add(HttpResponseEncoder())
                            handlers.add(WebSocketServerProtocolHandler("/"))
                            handlers.add(audioServerHandler)

                            ch.pipeline().addLast(*handlers.toTypedArray())
                        }
                    })

                ch = sb.bind(InetSocketAddress("0.0.0.0", port)).sync().channel()
                PluginProvider.getInstance().logger.info("AudioServer started on port $port")

                ch!!.closeFuture().sync()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                bossGroup!!.shutdownGracefully()
                workerGroup!!.shutdownGracefully()
            }
        }, "cv-audioserver")
        serverThread.start()
    }

    override fun hasJoined(player: Player): Boolean {
        for (channelMetaData in AudioServerHandler.channelMetaData) {
            if (channelMetaData.player === player) {
                return true
            }
        }
        return false
    }

    override fun sendPacket(player: Player, basePacket: BasePacket): Boolean {
        for (channelMetaData in AudioServerHandler.channelMetaData) {
            if (channelMetaData.player === player) {
                basePacket.send(channelMetaData)
                return true
            }
        }
        return false
    }

    override fun disconnect(player: Player, message: String) {
        AudioServerHandler.disconnect(player, message)
    }

    override fun stop() {
        if (ch != null) {
            val cf = ch!!.close()
            cf.awaitUninterruptibly()
        }
        bossGroup!!.shutdownGracefully()
        workerGroup!!.shutdownGracefully()

        serverThread.interrupt()
    }
}
