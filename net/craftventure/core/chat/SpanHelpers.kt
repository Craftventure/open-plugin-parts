package net.craftventure.core.chat

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.span.PlayerTagSpan
import net.craftventure.chat.bungee.span.RideChatSpan
import net.craftventure.chat.bungee.util.AnnotatedChat
import net.craftventure.chat.bungee.util.AnnotatedChatUtils.playerTagRegex
import net.craftventure.core.inventory.impl.RidesMenu.Companion.representAsItemStack
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.SoundCategory

object SpanHelpers {

    fun AnnotatedChat.handleRideTags() {
        val rides = MainRepositoryProvider.rideRepository.cachedItems
        for (ride in rides) {
            val occurences =
                Regex.escape("ride:${ride.name}").toRegex(RegexOption.IGNORE_CASE).findAll(this.text)
            occurences.forEach { occurence ->
                val representation = ride.representAsItemStack(null, null)
                val component: Component = (representation.itemStack.lore ?: emptyList()).map {
                    PlainTextComponentSerializer.plainText().deserialize(it)
                }.let { parts ->
                    var result = Component.text("")
                    parts.forEach { part -> result += part }
                    result
                }
                this.setSpan(
                    RideChatSpan(
                        occurence.range.start,
                        occurence.range.endInclusive,
                        representation.ride.displayName ?: "?",
                        representation.warp?.id,
                        component
                    )
                )
            }
        }
    }

    fun AnnotatedChat.handlePlayerTags() {
        playerTagRegex.findAll(this.text).forEach { occurence ->
//                Logger.debug(
//                    "Tag found ${occurence.value} ${occurence.range.start} ${occurence.range.endInclusive} [${occurence.groupValues.joinToString(
//                        ","
//                    )}]"
//                )
            val taggedPlayer = occurence.groups["name"]?.let { Bukkit.getPlayerExact(it.value) }
            if (taggedPlayer != null)
                this.setSpan(
                    PlayerTagSpan(
                        occurence.range.start,
                        occurence.range.endInclusive,
                        taggedPlayer.name
                    ) {
                        taggedPlayer.playSound(
                            taggedPlayer.location,
                            "gui.messageroller",
                            SoundCategory.AMBIENT,
                            1f,
                            1f
                        )
                    }
                )
        }
    }
}