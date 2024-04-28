package net.craftventure.core.utils

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.Region
import org.bukkit.entity.Player

object WorldEditUtils {
    fun Player.getSelectedRegion(): Region? {
        val wePlayer = BukkitAdapter.adapt(player)
        val weSession = WorldEdit.getInstance().sessionManager.get(wePlayer)
        if (!weSession.isSelectionDefined(wePlayer.world)) {
            return null
        }
        return try {
            weSession.getSelection(weSession.selectionWorld)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}