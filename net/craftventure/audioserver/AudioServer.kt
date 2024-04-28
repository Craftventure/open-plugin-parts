package net.craftventure.audioserver

import net.craftventure.audioserver.commands.CommandAudio
import net.craftventure.audioserver.commands.CommandAudioDebug
import net.craftventure.audioserver.commands.CommandVolume
import net.craftventure.audioserver.config.AudioServerConfig
import net.craftventure.audioserver.event.AudioServerConnectedEvent
import net.craftventure.audioserver.listener.MainListener
import net.craftventure.audioserver.packet.BasePacket
import net.craftventure.audioserver.packet.PacketReload
import net.craftventure.audioserver.websocket.AudioServerHandler
import net.craftventure.bukkit.ktx.coroutine.executeSync
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.plugin.Environment
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.json.MoshiBase
import net.craftventure.core.ktx.json.toJson
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.io.File
import java.net.URLEncoder

class AudioServer {
    var audioServerConfig: AudioServerConfig? = null
        private set
    var audioServer: Server? = null
        private set

    var operatorDelegate: ((player: Player, rideId: String, controlId: String) -> Unit)? = null
    var audioServerWebsite: String? = null
    var audioServerSocket: String? = null

    fun reloadConfig() {
        try {
            val directory = File(PluginProvider.getInstance().dataFolder, "data/audio/")
            val adapter = MoshiBase.moshi.adapter(AudioServerConfig::class.java)
            val files = directory.walkTopDown().filter { it.isFile && it.extension == "json" }.toList()
//            Logger.debug("Files ${files.joinToString(", ") { it.path }}")
            val configs = files.mapNotNull {
                try {
                    it to adapter.fromJson(it.readText())
                } catch (e: Exception) {
                    Logger.warn("Invalid Audio Config '${it.absolutePath}': ${e.message}", logToCrew = true)
                    Logger.capture(e)
                    null
                }
            }
            audioServerConfig = configs.fold(AudioServerConfig()) { initial, it ->
                try {
                    initial.mergeWith(it.second!!)
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to merge audio config ${it.first.path}: ${e.message}", e)
                }
            }

            

//            Logger.debug(adapter.toJson(audioServerConfig))
            //            Logger.console(getGsonExposed().toJson(audioServerConfig));
            for (audioArea in audioServerConfig!!.areas) {
                audioArea.validate()
                for (areaPartString in audioArea.areaPartNames) {
                    for (areaPart in audioServerConfig!!.areaParts) {
                        if (areaPart.name!!.equals(areaPartString, ignoreCase = true)) {
                            audioArea.addAreaPart(areaPart)
                        }
                    }
                }
            }
            //            AudioServerAreaTester.test();
        } catch (e: Exception) {
            Logger.severe("Failed to load audio config: ${e.message}", logToCrew = true)
            Logger.capture(e)
        }

    }

    fun restartServer(): Boolean {
        try {
            if (audioServer != null)
                audioServer!!.stop()
            audioServer =
                NettyServer(if (PluginProvider.environment == Environment.PRODUCTION) 8887 else 8888)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun register(executor: Any, command: String) {
        try {
            if (executor is CommandExecutor) {
                PluginProvider.getInstance().server.getPluginCommand(command)!!.setExecutor(executor)
            }
        } catch (e: Exception) {
            throw IllegalStateException(String.format("Failed to register CommandExecutor for command %s", command), e)
        }

        try {
            if (executor is TabCompleter) {
                PluginProvider.getInstance().server.getPluginCommand(command)!!.tabCompleter = executor
            }
        } catch (e: Exception) {
            throw IllegalStateException(String.format("Failed to register TabCompleter for command %s", command), e)
        }

    }

    fun onEnable() {
        _this = this
        audioServer =
            NettyServer(if (PluginProvider.environment == Environment.PRODUCTION) 8887 else 8888)
        reloadConfig()
        Bukkit.getServer().pluginManager.registerEvents(MainListener(), PluginProvider.getInstance())
        register(CommandAudio(), "audio")
        register(CommandVolume(), "volume")
        register(CommandAudioDebug(), "audiodebug")

        Bukkit.getScheduler().scheduleSyncRepeatingTask(PluginProvider.getInstance(), {
            if (audioServerConfig != null) {
                for (audioArea in audioServerConfig!!.areas) {
                    audioArea.update()
                }
            }
        }, 5, 5)

        Bukkit.getScheduler().scheduleSyncRepeatingTask(PluginProvider.getInstance(), {
            for (audioArea in audioServerConfig!!.areas) {
                for (player in ArrayList(audioArea.players)) {
                    if (!player.isConnected()) {
                        audioArea.removePlayer(player)
                    }
                }
            }
            for (areaPart in audioServerConfig!!.areaParts) {
                for (player in ArrayList(areaPart.players)) {
                    if (!player.isConnected()) {
                        areaPart.removePlayer(player)
                    }
                }
            }
        }, 1L, 20 * 60L)
    }

    fun sendAudioLink(player: Player, reminder: Boolean) {
        val link = MainRepositoryProvider.audioServerLinkRepository.getOrCreate(player.uniqueId)
        if (link == null || link.key == null) {
            player.sendMessage(CVTextColor.serverError + "Failed to retrieve AudioServer key. Please retry or contact the crew.")
            return
        }
        var website = audioServerWebsite
        if (website == null) website = "http://audioserver.craftventure.net/index.html"
        var socket: String? = null
        try {
            if (audioServerSocket != null)
                socket = URLEncoder.encode(audioServerSocket!!, "UTF-8")
        } catch (e: Exception) {
            if (PluginProvider.environment == Environment.DEVELOPMENT)
                e.printStackTrace()
        }

        val url = website +
                "?uuid=" + player.uniqueId.toString() +
                "&username=" + player.name +
                "&auth=" + link.key +
                if (socket != null) "&socket=$socket" else ""
        val message = Component
            .text(
                if (reminder)
                    "Click here to connect to the AudioServer now and experience the park with music, park photos, a park map and more!"
                else
                    "Click here to open the AudioServer", CVTextColor.serverNoticeAccent
            )
            .hoverEvent(Component.text("Click to open the AudioServer in your browser", CVTextColor.CHAT_HOVER))
            .clickEvent(ClickEvent.openUrl(url))
        player.sendMessage(message)
        //        player.sendMessage(ChatColor.GOLD + "Or open " + url + " in a browser");
    }

    fun onDisable() {
        audioServer!!.stop()
    }

    companion object {
        private var _this: AudioServer? = null

        val instance: AudioServer
            get() {
                if (_this == null) {
                    _this = AudioServer()
                }
                return _this!!
            }

        val server get() = instance.audioServer

        inline fun <reified T : BasePacket> broadcast(packet: T) {
            broadcast(packet.toJson())
        }

        fun broadcast(json: String) {
            AudioServerHandler.channelMetaData.forEach {
                it.send(json)
            }
        }


        fun reload() {
            instance.reloadConfig()
            for (channelMetaData in AudioServerHandler.channelMetaData) {
                val player = channelMetaData.player
                PacketReload().send(channelMetaData)
                if (player != null)
                    for (audioArea in instance.audioServerConfig!!.areas) {
                        audioArea.handleJoin(player)
                    }

                for (audioArea in instance.audioServerConfig!!.areas) {
                    audioArea.update()
                }

                if (player != null)
                    executeSync {
                        Bukkit.getServer().pluginManager.callEvent(
                            AudioServerConnectedEvent(
                                player,
                                channelMetaData
                            )
                        )
                    }
            }
        }
    }
}
