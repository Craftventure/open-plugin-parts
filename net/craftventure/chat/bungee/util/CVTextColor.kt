package net.craftventure.chat.bungee.util

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor


object CVTextColor {
    @JvmStatic
    val crew = TextColor.fromHexString("#0e8e8e")!!

    @JvmStatic
    val vip = TextColor.fromHexString("#8f368f")!!

    @JvmStatic
    val youtube = TextColor.fromHexString("#aa4141")!!

    @JvmStatic
    val guest = TextColor.fromHexString("#d7d7d7")!!

    @JvmStatic
    val serverError = TextColor.fromHexString("#bb0000")!!

    @JvmStatic
    val serverErrorAccent = TextColor.fromHexString("#f70000")!!

    @JvmStatic
    val achievement = NamedTextColor.DARK_GREEN!!

    @JvmStatic
    val achievementAccent = NamedTextColor.GREEN!!

    @JvmStatic
    val serverNotice = TextColor.fromHexString("#dbdb4e")!!

    @JvmStatic
    val serverNoticeAccent = TextColor.fromHexString("#ff9600")!!

    val subtle = TextColor.fromHexString("#c5c5c5")!!

    @JvmStatic
    val subtleLegacy = NamedTextColor.GRAY!!

    @JvmStatic
    val subtleAccent = TextColor.fromHexString("#646464")!!

    val colorPlaceholders = mapOf<String, TextColor>(
        "serverNotice" to serverNotice,
        "serverNoticeAccent" to serverNoticeAccent,
        "serverError" to serverError,
        "serverErrorAccent" to serverErrorAccent,
        "achievement" to achievement,
        "achievementAccent" to achievementAccent,
        "subtle" to subtle,
        "subtleLegacy" to subtleLegacy,
        "subtleAccent" to subtleAccent,
        "vip" to vip,
        "youtube" to youtube,
        "crew" to crew,
    )

    @JvmStatic
    val CHAT_HOVER = serverNoticeAccent

    @JvmStatic
    val BROADCAST_RIDE_VIP_OPEN = vip

    @JvmStatic
    val BROADCAST_RIDE_OPEN = NamedTextColor.GREEN!!

    @JvmStatic
    val BROADCAST_RIDE_CLOSED = serverError

    @JvmStatic
    val BROADCAST_RIDE_BROKEN_DOWN = serverError

    @JvmStatic
    val INVENTORY_TITLE = TextColor.fromHexString("#313131")!!

    @JvmStatic
    val MENU_DEFAULT_TITLE = serverNoticeAccent

    @JvmStatic
    val MENU_DEFAULT_TITLE_ACCENT = serverNotice

    @JvmStatic
    val MENU_DEFAULT_LORE = subtle

    @JvmStatic
    val MENU_DEFAULT_LORE_ACCENT = crew

    val menuLoreAccentAlt = TextColor.fromHexString("#35029c")!!

    @JvmStatic
    val MENU_DEFAULT_LORE_ERROR = serverError

    @JvmStatic
    val MENU_DEFAULT_LORE_ACTION = serverNotice
}
