package net.craftventure.bukkit.ktx.entitymeta

import net.kyori.adventure.text.Component

abstract class BaseMetadata {
    //    val creator = Logger.miniTrace(8)
    open fun onDestroy() {}

    open fun isValid(target: Any): Boolean = false

    abstract fun debugComponent(): Component?// = null
}