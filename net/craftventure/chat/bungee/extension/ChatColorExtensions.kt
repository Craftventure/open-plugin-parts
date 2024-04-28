package net.craftventure.chat.bungee.extension

operator fun net.md_5.bungee.api.ChatColor.plus(value: String?): String = toString() + value