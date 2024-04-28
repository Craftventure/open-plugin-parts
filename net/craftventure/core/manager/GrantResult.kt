package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.util.sendServerError
import net.craftventure.chat.bungee.util.CVTextColor
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

sealed class GrantResult {
    fun isAllowed() = this is Allow
    fun isNotAllowed() = this !is Allow
}

object Allow : GrantResult()
data class Deny constructor(val reason: String/*, val solve: (() -> Boolean)? = null*/) : GrantResult() {
    val errorComponent get() = Component.text(reason, CVTextColor.serverError)

//    operator fun plus(solve: () -> Boolean) = Deny(reason, solve)
}

fun Player.displayDenyResult(deny: Deny, prefix: String = "Denied") {
    sendServerError(Component.text("$prefix: ${deny.reason}"))
}