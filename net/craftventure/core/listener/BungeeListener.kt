package net.craftventure.core.listener

import com.google.common.io.ByteStreams
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.extension.asUuid
import net.craftventure.core.manager.visibility.VisibilityManager
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

class BungeeListener : PluginMessageListener {
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        try {
//            Logger.debug("Channel $channel")
//            Bukkit.broadcast(TextComponent("channel $channel"))

            val reader = ByteStreams.newDataInput(message)
            val subChannel = reader.readUTF()

            if (subChannel == "connect/host") {
                val uuid = reader.readUTF().asUuid()!!
                val host = reader.readUTF()

                if (host.contains("vanish", ignoreCase = false) || PluginProvider.isTestServer()) {
                    VisibilityManager.requestVanishJoin(uuid)
                }
            }
        } catch (e: Exception) {
            if (PluginProvider.isTestServer())
                e.printStackTrace()
        }
    }
}