package net.craftventure.core.listener

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.event.platform.CommandEvent
import com.sk89q.worldedit.event.platform.PlayerInputEvent
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.eventbus.Subscribe
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import kotlin.math.max


class WorldEditListener : Listener {
    init {
        WorldEdit.getInstance().eventBus.register(this)
    }

    private var selections = hashMapOf<UUID, CachedRegion>()

    private fun updateRegion(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return
        updateRegion(player)
    }

    private fun updateRegion(player: Player) {
        val plugin = Bukkit.getPluginManager().getPlugin("WorldEdit") as WorldEditPlugin
        val session = plugin.getSession(player)
        val selector = session.getRegionSelector(BukkitWorld(player.world))
        val region = try {
            selector.region
        } catch (e: Exception) {
            return
        }
        region.minimumPoint
        val existingRegion = selections[player.uniqueId]
//        Logger.debug("min=${region.minimumPoint} vs ${existingRegion?.minimumPoint} max=${region.maximumPoint} vs ${existingRegion?.maximumPoint}")
        if (region.minimumPoint != existingRegion?.minimumPoint || region.maximumPoint != existingRegion?.maximumPoint) {
//            Logger.debug("New region")
            selections[player.uniqueId] = CachedRegion(region.minimumPoint, region.maximumPoint)

            val x = region.maximumPoint.x - region.minimumPoint.x
            val y = region.maximumPoint.y - region.minimumPoint.y
            val z = region.maximumPoint.z - region.minimumPoint.z
            val totalBlocks = (x + 1) * (y + 1) * (z + 1)
            val maxSize = max(x, max(y, z))
            if (maxSize < 15 || totalBlocks < 500) return
            val titleColor = when {
                maxSize < 30 -> NamedTextColor.GRAY
                maxSize < 60 -> NamedTextColor.YELLOW
                maxSize < 100 -> NamedTextColor.RED
                else -> NamedTextColor.DARK_RED
            }
            if (totalBlocks > 5000) {
                player.playSound(player.location, SoundUtils.GUI_ERROR, 1f, 1f)
            }
            player.displayTitle(
                TitleManager.TitleData.ofTicks(
                    "worldedit",
                    title = Component.text("[$x, $y, $z]", titleColor),
                    subtitle = Component.text("That's $totalBlocks potential blocks", titleColor),
                    fadeInTicks = 0,
                    stayTicks = 20 * 3,
                    fadeOutTicks = 20,
                ),
                replace = true
            )
        } else {
//            Logger.debug("Same region")
        }
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        selections.remove(event.player.uniqueId)
    }

    @Subscribe(priority = com.sk89q.worldedit.util.eventbus.EventHandler.Priority.VERY_LATE)
    fun onPlayerInputEvent(event: PlayerInputEvent) {
//                if (event.actor?.isPlayer != true) return
//        Logger.debug("PlayerInputEvent ${event.player.isPlayer}")
        if (event.player.isPlayer)
            updateRegion(event.player.uniqueId)
    }

    @Subscribe(priority = com.sk89q.worldedit.util.eventbus.EventHandler.Priority.VERY_LATE)
    fun onPlayerInputEvent(event: CommandEvent) {
//                if (event.actor?.isPlayer != true) return
//        Logger.debug("CommandEvent ${event.actor.isPlayer}")
        if (event.actor.isPlayer)
            updateRegion(event.actor.uniqueId)
    }

    data class CachedRegion(
        val minimumPoint: BlockVector3,
        val maximumPoint: BlockVector3,
    )
}
