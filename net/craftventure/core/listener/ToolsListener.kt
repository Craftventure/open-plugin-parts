package net.craftventure.core.listener

import com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata
import com.comphenix.protocol.wrappers.WrappedDataValue
import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.extension.getMeta
import net.craftventure.core.extension.getPermissionName
import net.craftventure.core.inventory.impl.ArmorStandEditorMenu
import net.craftventure.core.inventory.impl.EntityEditorMenu
import net.craftventure.core.inventory.impl.TileStateEditorMenu
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.json.toJsonIndented
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.npc.EntityBitFlags
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.utils.BoundingBox
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.core.utils.Tools
import net.craftventure.core.utils.spawnParticleX
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.FluidCollisionMode
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.block.Skull
import org.bukkit.block.TileState
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.vehicle.VehicleEnterEvent


class ToolsListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onVehicleEnter(event: VehicleEnterEvent) {
        val player = event.entered as? Player ?: return
        if (!player.isCrew()) return
        if (player.gameMode != GameMode.CREATIVE) return
        val entity = event.vehicle
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.getMeta(Tools.KEY_TOOL_ID) == Tools.ENTITY_AND_TILE_STATE_EDITOR_ID) {
            EntityEditorMenu(player, entity).openAsMenu(player)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        if (!player.isCrew()) return
        if (player.isInsideVehicle || player.gameMode != GameMode.CREATIVE) return
        val entity = event.entity
//        Logger.debug("Entity damaged by crew: ${entity.type}")
        val forceOpenMenu = entity is ArmorStand
        val itemInHand = player.inventory.itemInMainHand
        if (forceOpenMenu || itemInHand.getMeta(Tools.KEY_TOOL_ID) == Tools.ENTITY_AND_TILE_STATE_EDITOR_ID) {
            if (entity is ArmorStand) {
                ArmorStandEditorMenu(player, entity).openAsMenu(player)
                event.isCancelled = true
            } else {
                EntityEditorMenu(player, entity).openAsMenu(player)
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onHangingBreakByEntityEvent(event: HangingBreakByEntityEvent) {
        val player = event.remover as? Player ?: return
        if (!player.isCrew()) return
        if (player.isInsideVehicle || player.gameMode != GameMode.CREATIVE) return
        val entity = event.entity
        val forceOpenMenu = entity is ItemFrame
        val itemInHand = player.inventory.itemInMainHand
        if (forceOpenMenu || itemInHand.getMeta(Tools.KEY_TOOL_ID) == Tools.ENTITY_AND_TILE_STATE_EDITOR_ID) {
            EntityEditorMenu(player, entity).openAsMenu(player)
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (!event.player.isCrew()) {
            return
        }
        val action = event.action
        val block = event.clickedBlock
        val itemStack = event.item
        val player = event.player

        val id = itemStack?.getMeta(Tools.KEY_TOOL_ID)
        if (id == Tools.ENTITY_AND_TILE_STATE_EDITOR_ID) {
            event.isCancelled = true
            if (block != null) {
                TileStateEditorMenu(player, block).openAsMenu(player)
            } else {
                player.sendMessage(CVTextColor.serverNotice + "No block selected")
            }
        } else if (id == Tools.POINTER_ID) {
            event.isCancelled = true
            val result = player.rayTraceBlocks(30.0, FluidCollisionMode.NEVER)
            result?.hitPosition?.spawnParticleX(player.world, Particle.END_ROD)
            if (player.isSneaking)
                result?.hitPosition?.spawnParticleX(player.world, Particle.EXPLOSION_HUGE)
        } else if (id == Tools.AUTOPIA_LOGGER_ID) {
            event.isCancelled = true
            if (PermissionChecker.isOwner(player)) {
                val location = player.location
                if (player.isSneaking) {
                    Logger.info(
                        "SpawnLocation(%.1f, %.1f, %.1f, %.1ff, %.1ff),".format(
                            location.x, location.y, location.z, location.yaw, location.pitch
                        ), logToCrew = false
                    )
                } else {
                    Logger.info(
                        "AutopiaTrackPoint(Vector(${location.x}, ${location.y}, ${location.z}), 6.0),",
                        logToCrew = false
                    )
                }
            }
        } else if (id == Tools.UUID_LOGGER_ID) {
            if (action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                val state = block?.state
                if (state is Skull) {
                    val id = state.owningPlayer?.uniqueId ?: return
                    Logger.info("UUID of block x=${block.x} y=${block.y} z=${block.z} $id")
                }
            }
        } else if (id == Tools.ENTITY_GLOWER_ID) {
            event.isCancelled = true
            val interactor = EntityMetadata.Entity.sharedFlags
            player.world.entities.forEach {
                val wrapperPlayServerEntityMetadata = WrapperPlayServerEntityMetadata()
                wrapperPlayServerEntityMetadata.entityID = it.entityId
                wrapperPlayServerEntityMetadata.metadata = listOf(
                    WrappedDataValue(
                        interactor.absoluteIndex!!,
                        interactor.wrappedSerializer,
                        EntityBitFlags.EntityState.GLOWING
                    )
                )
                wrapperPlayServerEntityMetadata.sendPacket(player)
            }
        } else if (id == Tools.BLOCKDATA_DEBUGGER_ID) {
            if (action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                val blockString = block!!.getRelative(0, 1, 0).blockData.getAsString(true)
                player.sendMessage(TextComponent().apply {
                    text =
                        "That block is '$blockString', click to copy. It's permission name is ${block.type.getPermissionName()} state=${block.state.javaClass.name}. Friction=${
                            block.getFrictionFactor()
                                ?.format(3)
                        } Color=rgb>${block.getMaterialColorRgb()?.toHexColor()}/map>${block.getMaterialColorForMap()}"
                    color = CVChatColor.serverNotice
                    clickEvent = ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, blockString)
                })

                (block.state as? TileState)?.persistentDataContainer?.apply {
                    this.keys.forEach {
                        player.sendMessage("Key $it")
                    }
                }
            }
        } else if (id == Tools.BB_DEBUGGER_ID) {
            if (action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                val blockLocation = block!!.location
                block.world.getBoundingBoxesForBlock(block.x, block.y, block.z)
                    ?.map { boundingBox ->
                        "x[${boundingBox.minX.format(2)} ${boundingBox.minX.format(2)}] " +
                                "y[${boundingBox.minY.format(2)} ${boundingBox.minY.format(2)}] " +
                                "z[${boundingBox.minZ.format(2)} ${boundingBox.minZ.format(2)}]"
                    }
                    ?.joinToString("\n")
                    ?.let {
                        player.sendMessage(it)
                    }
                val boundingBox = block.world.getBoundingBoxForBlock(block.x, block.y, block.z)
                if (boundingBox != null) {
                    val localBoundingBox = BoundingBox().set(boundingBox)
                    localBoundingBox.debug(blockLocation)
                    player.sendTitleWithTicks(
                        stay = 20 * 5,
                        subtitleColor = NamedTextColor.YELLOW,
                        subtitle = "x[${localBoundingBox.xMin.format(2)} ${localBoundingBox.xMax.format(2)}] " +
                                "y[${localBoundingBox.yMin.format(2)} ${localBoundingBox.yMax.format(2)}] " +
                                "z[${localBoundingBox.zMin.format(2)} ${localBoundingBox.zMax.format(2)}]"
                    )
                }
            }
        } else if (id == Tools.LOCATION_LOGGER_ID) {
            event.isCancelled = true

            val playerLocation =
                if (block != null && !player.isSneaking && (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK)) {
                    player.sendMessage("Using location from Clicked Block")
                    block.location
                } else {
                    if (player.isSneaking) {
                        player.sendMessage("Using location from the block the player is standing in")
                        player.location.toBlockLocation().add(0.5, 0.0, 0.5)
                    } else {
                        player.sendMessage("Using location from Player")
                        player.location
                    }
                }

            if (block != null)
                player.sendMessage(
                    Component.text(
                        "friction=${block.getFrictionFactor()?.format(2)} mapColor=#${
                            block.getMaterialColorRgb()?.toString(16)
                        } (${block.getMaterialColorForMap()})", CVTextColor.serverNotice
                    )
                )

            val locationString =
                "Location(Bukkit.getWorld(\"${player.world.name}\"), ${playerLocation.x.format(2)}, ${
                    playerLocation.y.format(2)
                }, ${playerLocation.z.format(2)}, ${playerLocation.yaw.format(2)}, ${
                    playerLocation.pitch.format(
                        2
                    )
                })"

            player.sendMessage(
                Component.text("Copy for Kotlin", CVTextColor.serverNotice)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(locationString))
            )

            player.sendMessage(
                Component.text("Copy for JSON", CVTextColor.serverNotice)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(playerLocation.toJsonIndented()))
            )

            player.sendMessage(
                Component.text("Copy for Soarin'", CVTextColor.serverNotice)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard("${playerLocation.x}/${playerLocation.y}/${playerLocation.z}/${playerLocation.pitch}/${playerLocation.yaw}/0.0/89.0"))
            )

            if (block != null)
                for (x in 0..3) {
                    for (y in 0..3) {
                        for (z in 0..3) {
                            block.world.spawnParticleX(
                                Particle.END_ROD,
                                block.x + x * 0.33,
                                block.y + y * 0.33,
                                block.z + z * 0.33
                            )
                        }
                    }
                }
        }
    }
}
