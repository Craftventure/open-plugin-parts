package net.craftventure.chat.bungee.util

import net.kyori.adventure.text.Component


abstract class ChatSpan(val startIndexInclusive: Int, val endIndexInclusive: Int) {
    open fun applyEffects() {}

    fun overlaps(other: ChatSpan) =
        other.startIndexInclusive <= startIndexInclusive && other.endIndexInclusive >= startIndexInclusive

    fun overlaps(index: Int) = startIndexInclusive <= index && index <= endIndexInclusive

    abstract fun appendTo(source: String, component: Component): Component
}