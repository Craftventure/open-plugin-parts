package net.craftventure.chat.bungee.util

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.core.util.SpaceHelper
import net.kyori.adventure.text.Component

object CvComponent {
    fun space(size: Int, noSplit: Boolean = false) =
        Component.text(SpaceHelper.width(size, noSplit = noSplit))

    fun resettingInventoryOverlay(code: UiElement) =
        Component.text(SpaceHelper.width(-8, noSplit = false)) +
                code.component +
                Component.text(SpaceHelper.width(-code.width - 1 + 8, noSplit = false))

    fun resettingInventoryOverlay(vararg codes: UiElement): Component {
        val component = Component.text()
        component.append(Component.text(SpaceHelper.width(-8, noSplit = false)))

        codes.forEach { code ->
            component.append(code.component)
            component.append(Component.text(SpaceHelper.width(-code.width - 1, noSplit = false)))
        }
        component.append(Component.text(SpaceHelper.width(8, noSplit = false)))

        return component.build()
    }
}