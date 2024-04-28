package net.craftventure.core.map.fonts

import org.bukkit.map.MapFont

// Minecraftified version of Minguarana font by https://www.deviantart.com/matiasromero
// Minguarana was the main font of CV

class MinguaranaFont private constructor() : MapFont() {
    companion object {
        @JvmStatic
        val FONT = MinguaranaFont()
    }
}
