package net.craftventure.bukkit.ktx.manager

import com.squareup.moshi.adapter
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.util.AnnotatedChatUtils
import net.craftventure.core.ktx.json.MoshiBase
import net.craftventure.core.ktx.logging.logException
import java.io.File

object EmojiLoader {
    @OptIn(ExperimentalStdlibApi::class)
    fun loadEmojiMappings() {
        val file = File(PluginProvider.plugin.dataFolder, "twemoji.json")
        if (file.exists()) {
            try {
                AnnotatedChatUtils.emojis = MoshiBase.moshi.adapter<Map<String, String>>().fromJson(file.readText())!!
            } catch (e: Exception) {
                logException(e)
            }
        }
    }
}