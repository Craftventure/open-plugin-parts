package net.craftventure.chat.bungee.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags

private val tagBuilder = TagResolver.builder()
    .resolver(StandardTags.defaults())
    .apply {
        CVTextColor.colorPlaceholders.forEach { color ->
            val key = color.key.replace("([A-Z]+)([A-Z][a-z])".toRegex(), "$1_$2")
                .replace("([a-z])([A-Z])".toRegex(), "$1_$2").lowercase()
//            logcat { "Resolving $key to color" }
            resolver(TagResolver.resolver(key, Tag.styling(color.value)))
        }
    }

private fun rebuild() = MiniMessage
    .builder()
    .tags(
        tagBuilder
            .build()
    )
    .build()

var CvMiniMessage = rebuild()
    private set

fun rebuildCvMiniMessage(builder: TagResolver.Builder.() -> Unit) {
    builder(tagBuilder)
    CvMiniMessage = rebuild()
}

fun String.parseWithCvMessage() = try {
    CvMiniMessage.deserialize(this.replace("\r\n", "<newline>").replace("\n", "<newline>"))
} catch (e: Exception) {
    e.printStackTrace()
    Component.text("((Failed to parse: $this))")
}