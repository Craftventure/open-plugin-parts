package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.core.extension.boundingBox
import net.craftventure.core.map.renderer.MapManager
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.meta.MapMeta

class MapListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPacketUseEntityEvent(event: PlayerInteractAtEntityEvent) {
        val target = event.clickedPosition
        val entity = try {
            event.player.world.entities.toTypedArray()
                .firstOrNull { it.entityId == event.rightClicked.entityId } as? ItemFrame
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (entity != null) {
            val facing = entity.facing
            if (facing != BlockFace.NORTH && facing != BlockFace.EAST && facing != BlockFace.SOUTH && facing != BlockFace.WEST)
                return

            val bounding = entity.boundingBox()
            val clickLocation = entity.location.clone().add(target)
            val padding = 0.13//(1 - (bounding.xMax() - bounding.xMin())) / 2
            val itemFrameSize = 1 - (padding * 2)

            val xPercentage = ((clickLocation.x - bounding.xMin()) / (bounding.xMax() - bounding.xMin()))
            val yPercentage = 1 - ((clickLocation.y - bounding.yMin()) / (bounding.yMax() - bounding.yMin()))
            val zPercentage = ((clickLocation.z - bounding.zMin()) / (bounding.zMax() - bounding.zMin()))

            val correctedXPercentage = ((when (facing) {
                BlockFace.NORTH -> 1 - xPercentage
                BlockFace.EAST -> 1 - zPercentage
                BlockFace.SOUTH -> xPercentage
                BlockFace.WEST -> zPercentage
                else -> xPercentage
            } * itemFrameSize) + padding) / 1
            val correctedYPercentage = ((yPercentage * itemFrameSize) + padding) / 1

//                Logger.info("Clicked at ${correctedXPercentage.format(2)} ${correctedYPercentage.format(2)}")

            val item = entity.item
            val meta = item.itemMeta
            if (meta is MapMeta) {
                if (MapManager.instance.clicked(
                        event.player,
                        meta.mapId,
                        correctedXPercentage,
                        correctedYPercentage
                    )
                ) {
                    event.isCancelled = true
                }
            }
//                Logger.info("${xPercentage.format(2)} ${yPercentage.format(2)} ${zPercentage.format(2)} ${padding.format(2)}")

//                Logger.info("$padding ${bounding.xMin().format(2)} ${bounding.xMax().format(2)} " +
//                        "y=${bounding.yMin().format(2)} ${bounding.yMax().format(2)} " +
//                        "z=${bounding.zMin().format(2)} ${bounding.zMax().format(2)} " +
//                        "x=${target.x.format(2)} y=${target.y.format(2)} z=${target.z.format(2)} " +
//                        "$facing")


//                val location = entity.location.clone()
//                location.add(target)
//                location.spawnParticle<Any>(
//                        particle = Particle.END_ROD,
//                        count = 1
//                )
        }
    }
}