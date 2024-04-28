package net.craftventure.chat.bungee.util

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.core.ktx.util.Logger
import net.kyori.adventure.text.Component

class AnnotatedChat(val text: String) {
    var spans: List<ChatSpan> = emptyList()
        private set

    fun annotatedParts(): List<Pair<String, ChatSpan?>> {
        if (spans.isEmpty()) return listOf(text to null)
        val annotatedParts = mutableListOf<Pair<String, ChatSpan?>>()
        var index = 0
        if (ChatHelpers.DEBUG_COMPILING)
            Logger.debug("Starting compiling annotation parts")
        while (index < text.length) {
            if (ChatHelpers.DEBUG_COMPILING)
                Logger.debug("Checking parts for index $index")
            val part = spans.firstOrNull { it.overlaps(index) }
            if (part != null) {
                if (ChatHelpers.DEBUG_COMPILING)
                    Logger.debug("Part ${part.startIndexInclusive} - ${part.endIndexInclusive}")
                annotatedParts.add(text.substring(part.startIndexInclusive, part.endIndexInclusive + 1) to part)
                index = part.endIndexInclusive + 1
                if (ChatHelpers.DEBUG_COMPILING)
                    Logger.debug("Index set to $index")
            } else {
                val nextPart = spans.firstOrNull { it.startIndexInclusive > index }
                if (nextPart != null) {
                    if (ChatHelpers.DEBUG_COMPILING)
                        Logger.debug("Next part")
                    annotatedParts.add(text.substring(index, nextPart.startIndexInclusive) to null)
                    index = nextPart.startIndexInclusive
                    if (ChatHelpers.DEBUG_COMPILING)
                        Logger.debug("Index set to $index")
                } else {
                    if (ChatHelpers.DEBUG_COMPILING)
                        Logger.debug("Finished")
                    annotatedParts.add(text.substring(index) to null)
                    index = text.length
                    if (ChatHelpers.DEBUG_COMPILING)
                        Logger.debug("Index set to $index")
                }
            }
        }
        return annotatedParts
    }

    fun setSpan(span: ChatSpan): Boolean {
        if (spans.any { it.overlaps(span) }) {
            if (ChatHelpers.DEBUG_COMPILING)
                Logger.debug("Part $span is invalid")
            return false
        }
        spans = (spans + span).sortedBy { it.startIndexInclusive }
        return true
    }

    fun partAtIndex(index: Int) =
        spans.firstOrNull { it.startIndexInclusive <= index && it.endIndexInclusive > index }

    fun appendTo(component: Component): Component {
        var result = component
        annotatedParts().forEach { (text, part) ->
            if (part != null) {
                result += part.appendTo(text, component)
                part.applyEffects()
            } else {
                result += Component.text(text)
            }
        }
        return result
    }
}