package net.craftventure.core.map.renderer

import org.bukkit.entity.Player

interface InteractableRenderer {
    fun interact(player: Player, mapId: Int, x: Double, y: Double)
}