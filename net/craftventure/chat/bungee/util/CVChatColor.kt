package net.craftventure.chat.bungee.util

import net.md_5.bungee.api.ChatColor

@Deprecated("Use CVTextColor instead")
object CVChatColor {
    val serverError = CVTextColor.serverError.asHexString().asChatColor()

    val serverNotice = CVTextColor.serverNotice.asHexString().asChatColor()

    //    @Deprecated(message = "Use the color directly")
    val MENU_DEFAULT_TITLE = CVTextColor.MENU_DEFAULT_TITLE.asHexString().asChatColor()

    //    @Deprecated(message = "Use the color directly")
    val MENU_DEFAULT_TITLE_ACCENT = CVTextColor.MENU_DEFAULT_TITLE_ACCENT.asHexString().asChatColor()

    //    @Deprecated(message = "Use the color directly")
    val MENU_DEFAULT_LORE = CVTextColor.MENU_DEFAULT_LORE.asHexString().asChatColor()

    //    @Deprecated(message = "Use the color directly")
    val MENU_DEFAULT_LORE_ACCENT = CVTextColor.MENU_DEFAULT_LORE_ACCENT.asHexString().asChatColor()

    //    @Deprecated(message = "Use the color directly")
    val MENU_DEFAULT_LORE_ERROR = CVTextColor.MENU_DEFAULT_LORE_ERROR.asHexString().asChatColor()

    //    @Deprecated(message = "Use the color directly")
    val MENU_DEFAULT_LORE_ACTION = CVTextColor.MENU_DEFAULT_LORE_ACTION.asHexString().asChatColor()

    fun String.asChatColor() = ChatColor.of(this)
}
