package net.craftventure.core.feature.kart.actions

import net.craftventure.core.feature.kart.Kart
import org.bukkit.entity.Player

fun interface ExitHandler {
    fun onExit(kart: Kart, player: Player)
}