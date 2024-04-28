package net.craftventure.chat.bungee.util

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CvComponent.space
import net.craftventure.chat.core.util.SpaceHelper
import net.craftventure.core.ktx.util.GlyphSizes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object FontUtils {
    fun width(component: Component): Int {
        val message = PlainTextComponentSerializer.plainText().serialize(component)
        return GlyphSizes.calculateWidth(message)
    }

    fun width(message: String): Int {
        return GlyphSizes.calculateWidth(message)
    }

    fun wrapInUnderlay(component: Component, noSplit: Boolean = false): Component {
        val message = PlainTextComponentSerializer.plainText().serialize(component)
//        Logger.debug("Wrapping [$message]")
        return wrapInUnderlay(component, GlyphSizes.calculateWidth(message), noSplit = noSplit)
    }

    fun centerForWidth(component: Component, width: Int): Component {
        val componentWidth = width(component)
        if (componentWidth > width) return component
        val offset = (width - componentWidth) / 2
//        Logger.debug("Offset $offset for componentWidth=$componentWidth width=$width")
        return Component.text()
            .append(space(offset))
            .append(component)
            .build()
    }

    fun wrapInUnderlay(
        messageComponent: Component,
        renderWidth: Int,
        noSplit: Boolean = false,
    ): Component {
        val sizedSpace = if (noSplit) SpaceHelper.NegativeNoSplit else SpaceHelper.Negative
        if (renderWidth == 0) return Component.empty()
        var component = Component.text("")
//        Logger.debug("Wrapping width $renderWidth")
        var lengthLeft = renderWidth
        var underlay = "\uE04E${sizedSpace.s1}"
        while (lengthLeft > 0) {
            when (lengthLeft) {
                in 48..Int.MAX_VALUE -> {
                    underlay += '\uE05B'
                    lengthLeft -= 48
                }

                in 32..Int.MAX_VALUE -> {
                    underlay += "\uE05A"
                    lengthLeft -= 32
                }

                in 16..Int.MAX_VALUE -> {
                    underlay += "\uE059"
                    lengthLeft -= 16
                }

                in 10..Int.MAX_VALUE -> {
                    underlay += "\uE058"
                    lengthLeft -= 10
                }

                in 1..9 -> {
                    underlay += ('\uE04E'.toInt() + lengthLeft).toChar().toString()
                    lengthLeft = 0
                }
            }
            underlay += sizedSpace.s1
        }
        component += Component.text(underlay)
        component += SpaceHelper.width(-renderWidth, noSplit = noSplit)

        component += messageComponent

        component += "\uE05C"
        return component
    }
}