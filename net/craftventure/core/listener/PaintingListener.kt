package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.core.ktx.util.Permissions
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import org.bukkit.Art
import org.bukkit.Material
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*


class PaintingListener : Listener {
//    @EventHandler
//    fun onPlayerItemHeldChanged(event: PlayerItemHeldEvent) {
//        if (!event.player.isCrew()) return
//        if (!event.player.isSneaking) return
//        val itemInHand = event.player.inventory.getItem(event.previousSlot) ?: return
//        val meta = itemInHand.itemMeta as? MapMeta ?: return
//        //        Logger.info("Durability=${itemInHand.durability} ${event.newSlot > event.previousSlot}")
//        if (event.newSlot > event.previousSlot && meta.mapId < 999) {
//            meta.mapId++
//        } else if (event.newSlot < event.previousSlot && meta.mapId > 1) {
//            meta.mapId--
//        }
//        itemInHand.itemMeta = meta
//
////        event.isCancelled = true
//        event.player.inventory.heldItemSlot = event.previousSlot
//    }

    @EventHandler
    fun onPaintingPlace(event: HangingPlaceEvent) {
        if (event.entity is Painting) {
            val painting = event.entity as Painting
            painting.art = Art.KEBAB
        }
    }

    @EventHandler
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (event.hand == EquipmentSlot.HAND &&
            event.rightClicked is Painting &&
            player.inventory.itemInMainHand.type == Material.PAINTING &&
            player.hasPermission(Permissions.CREW)
        ) {
            val painting = event.rightClicked as Painting
            val allArts = Art.values().toList().let { if (player.isSneaking) it.reversed() else it }
            val indexOfArt = allArts.indexOf(painting.art)
            val sortedArts = (allArts.subList(indexOfArt, allArts.size) + allArts.subList(
                0,
                indexOfArt
            )).filter { it != painting.art }
//            Logger.info("\nFrom $indexOfArt\n${allArts.joinToString(", ")}\nto\n${sortedArts.joinToString(", ")}")

            sortedArts.forEach { artToSet ->
                if (painting.setArt(artToSet)) {
                    player.sendTitleWithTicks(
                        0,
                        20 * 2,
                        subtitle = "Art changed to ${
                            artToSet.name.lowercase(Locale.getDefault())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
                    )
                    return
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val entity = event.entity as? ItemFrame ?: return
        val damager = event.damager as? Player ?: return

        if (damager.isCrew()) {
            val item = entity.item
            if (item != null) {
                entity.setItem(null)
            }
        }
    }
}
