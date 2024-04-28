package net.craftventure.core.feature.kart

import org.bukkit.entity.Player

interface KartAction {
    fun execute(kart: Kart, type: Type, target: Player? = null)

    enum class Type {
        LEFT_CLICK,
        RIGHT_CLICK
    }
}
