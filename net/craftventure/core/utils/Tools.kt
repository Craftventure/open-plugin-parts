package net.craftventure.core.utils

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import org.bukkit.NamespacedKey

object Tools {
    val KEY_TOOL_ID by lazy { NamespacedKey(CraftventureCore.getInstance(), "toolsId") }

    val LOCATION_LOGGER = CVTextColor.serverNoticeAccent + "The Location Logger"
    val LOCATION_LOGGER_ID = "locationlogger"

    val AUTOPIA_LOGGER = CVTextColor.serverNoticeAccent + "The Autopia Tracklogger"
    val AUTOPIA_LOGGER_ID = "autopiatracklogger"

    val PROTECTION_TOOL = CVTextColor.serverNoticeAccent + "The Protector"
    val PROTECTION_TOOL_ID = "protector"

    val BB_DEBUGGER = CVTextColor.serverNoticeAccent + "The Bounder"
    val BB_DEBUGGER_ID = "bounder"

    val BLOCKDATA_DEBUGGER = CVTextColor.serverNoticeAccent + "The Blocker"
    val BLOCKDATA_DEBUGGER_ID = "blocker"

    val UUID_LOGGER = CVTextColor.serverNoticeAccent + "UUID Logger"
    val UUID_LOGGER_ID = "uuidlogger"

    val ENTITY_GLOWER = CVTextColor.serverNoticeAccent + "Entity Glower"
    val ENTITY_GLOWER_ID = "entityglower"

    val ENTITY_AND_TILE_STATE_EDITOR = CVTextColor.serverNoticeAccent + "Entity & TileState editor"
    val ENTITY_AND_TILE_STATE_EDITOR_ID = "entitytilestateeditor"

    val POINTER = CVTextColor.serverNoticeAccent + "Laser Pointer"
    val POINTER_ID = "laserpointer"
}
