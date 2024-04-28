package net.craftventure.bukkit.ktx.util

import net.craftventure.core.ktx.util.Logger
import net.kyori.adventure.text.format.NamedTextColor

class BukkitLogger {
    companion object {
        val Logger.Level.color
            get() = when (this) {
                Logger.Level.SEVERE -> NamedTextColor.DARK_RED
                Logger.Level.WARNING -> NamedTextColor.YELLOW
                Logger.Level.INFO -> NamedTextColor.WHITE
                Logger.Level.DEBUG -> NamedTextColor.GRAY
            }
    }
}