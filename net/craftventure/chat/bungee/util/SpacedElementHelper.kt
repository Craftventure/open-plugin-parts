package net.craftventure.chat.bungee.util

import net.craftventure.chat.core.util.SpaceHelper
import net.kyori.adventure.text.Component

class SpacedElementHelper {
    var currentX = 0
        private set
    var builder = Component.text()
        private set
    val component get() = builder.build()

    fun add(x: Int, element: UiElement, component: Component = element.component) {
        val diff = x - currentX
        if (diff != 0) {
            builder.append(Component.text(SpaceHelper.width(diff)))
        }
        builder.append(component)
        currentX = x + element.width + 1
    }

    fun moveToX(target: Int) {
        val diff = target - currentX
        if (diff != 0) {
            builder.append(Component.text(SpaceHelper.width(diff)))
        }
        currentX = 0
    }

    fun reset() {
        builder = Component.text()
    }
}