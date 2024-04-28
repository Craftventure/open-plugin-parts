package net.craftventure.core.utils

import net.craftventure.chat.bungee.util.AnnotatedChatUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.extension.actualAliases
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object EmojiHelper {
    fun getAllEmojis() =
        AnnotatedChatUtils.emojis.keys + MainRepositoryProvider.emojiRepository.cachedItems.flatMap { it.actualAliases.toList() }

    fun updateEmojis(player: Player) {
        player.addAdditionalChatCompletions(getAllEmojis())
    }

    fun updateEmojisForAllPlayers() {
        val allEmojis = getAllEmojis()
        Bukkit.getOnlinePlayers().forEach { player ->
            player.addAdditionalChatCompletions(allEmojis)
        }
    }
}