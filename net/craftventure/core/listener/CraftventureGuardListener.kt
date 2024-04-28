package net.craftventure.core.listener

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent
import net.craftventure.bukkit.ktx.event.FeatureToggledEvent
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.extension.isOpenable
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.bukkit.ktx.util.ItemStackUtils2
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.AsyncTask
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.displayNoFurther
import net.craftventure.core.extension.getPermissionName
import net.craftventure.core.ktx.extension.random
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.MailManager
import net.craftventure.core.manager.PlayerStateManager.gameState
import net.craftventure.core.manager.visibility.VisibilityManager
import net.craftventure.core.utils.LookAtUtil
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.getNearestWarpFromLocation
import net.craftventure.database.bukkit.extensions.toLocation
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.TileState
import org.bukkit.entity.*
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.messaging.PluginMessageListener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*


class CraftventureGuardListener : Listener, PluginMessageListener {

    fun getMaterialName(material: Material): String {
        return material.getPermissionName()
        //        return material.name().toLowerCase().replace("_", "");
    }

    fun getEntityTypeName(entity: Entity): String {
        return entity.type.name.lowercase(Locale.getDefault())
    }

    fun getDamageCauseName(cause: EntityDamageEvent.DamageCause): String {
        return cause.name.lowercase(Locale.getDefault())
    }

    //    @EventHandler(ignoreCancelled = true)
    //    public void onEntityDismountEvent(EntityDismountEvent event) {
    //        if (event.getPlayer() instanceof Player) {
    //            Logger.console("SPIGOT UNMOUNT");
    //        }
    //    }
    //
    //    @EventHandler(ignoreCancelled = true)
    //    public void onEntityDismountEvent(PacketPlayerSteerEvent event) {
    //        if (event.isDismounting()) {
    //            Logger.console("PACKET UNMOUNT");
    //        }
    //    }

    //    @EventHandler
    //    public void onVehicleEntityCollision(final VehicleEntityCollisionEvent event) {
    //        event.setCancelled(true);
    //        event.setCollisionCancelled(true);
    //        event.setPickupCancelled(true);
    ////        Logger.console("VehicleEntityCollisionEvent");
    //    }


    @EventHandler(priority = EventPriority.LOWEST)
    fun onBoatDamage(event: VehicleDamageEvent) {
        if (event.attacker is Player) {
            if (!event.attacker!!.hasPermission("cvguard.damage.vehicle.${getEntityTypeName(event.vehicle)}"))
                event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCraftItem(e: CraftItemEvent) {
        if (e.whoClicked is Player) {
            val who = e.whoClicked as Player
            if (!who.hasPermission("cvguard.craft"))
                e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerSwapHandItems(e: PlayerSwapHandItemsEvent) {
        if (!e.player.hasPermission("cvguard.swapitems"))
            e.isCancelled = true
        //        if (!e.getPlayer().hasPermission("cvguard.swapitem." + e.getMainHandItem().getType().name() + "." + e.getOffHandItem().getType().name()))
        //            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onVehicleCollision(event: VehicleEntityCollisionEvent) {
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerActualMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        val positionChanged = from.x != to.x || from.y != to.y || from.z != to.z
        if (!positionChanged) return
        if (!CraftventureCore.getSettings().borderFor(event.player).isInArea(event.to)) {
            event.isCancelled = true
            event.player.displayNoFurther()
            //            event.getPlayer().sendMessage(CVChatColor.COMMAND_ERROR + "You can't go there!");
        }

        if (!event.player.isInsideVehicle) {
            var targetBlock = event.player.location.block
            if (!targetBlock.isOpenable())
                targetBlock = targetBlock.getRelative(0, -1, 0)

            if (targetBlock.isOpenable()) {
                var possibleSign = targetBlock.getRelative(0, -2, 0)
                if (possibleSign.state !is Sign) {
                    possibleSign = targetBlock.getRelative(0, -3, 0)
                }

                if (possibleSign.state is Sign) {
                    val signState = possibleSign.state as Sign

                    val isTurnStile = "[turnstile]" == signState.getLine(3)
                    val isOperator = "[operator]" == signState.getLine(3)
                    val isBlockAll = "[blockall]" == signState.getLine(3)

                    if (isTurnStile || isOperator || isBlockAll) {
                        if (signState.data is org.bukkit.material.Sign) {
                            if (isBlockAll) {
                                event.player.displayNoFurther()
                                event.isCancelled = true
                                return
                            }
                            if (isOperator && PermissionChecker.isVIP(event.player)) {
                                return
                            }
                            val sign = signState.data as org.bukkit.material.Sign
                            val face = sign.facing

                            val from = event.from
                            val to = event.to

                            val xDelta = to.x - from.x
                            val zDelta = to.z - from.z

                            if (face == BlockFace.NORTH && zDelta > 0 ||
                                face == BlockFace.EAST && xDelta < 0 ||
                                face == BlockFace.SOUTH && zDelta < 0 ||
                                face == BlockFace.WEST && xDelta > 0
                            ) {
                                event.player.displayNoFurther()
                                event.isCancelled = true
                                if (isOperator) {
                                    event.player.sendTitleWithTicks(
                                        0, 20 * 4, 20,
                                        NamedTextColor.DARK_PURPLE, "Operator Only",
                                        NamedTextColor.DARK_PURPLE, "You need VIP to go past here"
                                    )
                                }
                            }
                            // north -z
                            // east +x
                            // south +z
                            // west -x
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onArmorSlot(event: InventoryClickEvent) {
//        Logger.info(
//            "Inventory click ${event.slotType} ${event.slot}, ${event.clickedInventory == event.whoClicked.inventory} ${event.inventory == event.whoClicked.inventory}" +
//                    "${event.inventory.type} ${event.inventory.holder} " +
//                    "${event.clickedInventory?.type} ${event.clickedInventory?.holder}"
//        )
        event.clickedInventory?.holder?.let {
            if (it is LivingEntity && it !is Player) {
                if (!event.whoClicked.hasPermission("inventory.interact.${it.type.name.lowercase(Locale.getDefault())}")) {
                    event.isCancelled = true
                    return@let
                }
            }
        }

        val isSelfInventory =
            event.clickedInventory === event.whoClicked.inventory || event.inventory === event.whoClicked.inventory
        if (event.slotType == InventoryType.SlotType.CRAFTING ||
            event.slotType == InventoryType.SlotType.ARMOR ||
            event.slotType == InventoryType.SlotType.FUEL ||
            event.slotType == InventoryType.SlotType.QUICKBAR ||
            isSelfInventory
        ) {
            if (event.whoClicked.gameMode == GameMode.CREATIVE && PermissionChecker.isCrew(event.whoClicked))
                return
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCreatureSpawn(event: PreCreatureSpawnEvent) {
        if (event.reason == CreatureSpawnEvent.SpawnReason.LIGHTNING ||
            event.reason == CreatureSpawnEvent.SpawnReason.SPAWNER ||
            event.reason == CreatureSpawnEvent.SpawnReason.MOUNT ||
            event.reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
            event.reason == CreatureSpawnEvent.SpawnReason.JOCKEY
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val firstAidWarp =
            MainRepositoryProvider.warpRepository.cachedItems.filter { it.id!!.startsWith("firstaid") }.random()
        if (firstAidWarp != null) {
            event.respawnLocation = firstAidWarp.toLocation()
            return
        }
        val warp = MainRepositoryProvider.warpRepository.getNearestWarpFromLocation(event.player.location, event.player)
        if (warp != null) {
            event.respawnLocation = warp.toLocation()
        } else {
            event.respawnLocation = event.player.world.spawnLocation
        }
    }

    @EventHandler
    fun onPlayerDeathEventBedLocation(event: PlayerDeathEvent) {
        val player = event.entity as? Player ?: return

        val firstAidWarp =
            MainRepositoryProvider.warpRepository.cachedItems.filter { it.id!!.startsWith("firstaid") }.random()
        if (firstAidWarp != null) {
            player.setBedSpawnLocation(firstAidWarp.toLocation(), true)
            return
        }
        val warp = MainRepositoryProvider.warpRepository.getNearestWarpFromLocation(player.location, player)
        if (warp != null) {
            player.setBedSpawnLocation(warp.toLocation(), true)
        } else {
            player.setBedSpawnLocation(player.world.spawnLocation, true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        //        Logger.info("Login for " + event.getPlayer().getName());
        //        long start = System.currentTimeMillis();
        //        if (!Permissions.isCrew(player)) {
        //            player.teleport(event.getPlayer().getWorld().getSpawnLocation());
        //            new AsyncResultTask() {
        //                private Warp warp;
        //
        //                @Override
        //                public void doInBackground() {
        //                    warp = CraftventureCore.getWarpDatabase().findSilent("spawn");
        //                }
        //
        //                @Override
        //                public void onPostExecute() {
        //                    if (warp != null) {
        //                        player.teleport(warp.toLocation());
        //                    } else {
        //                        player.teleport(event.getPlayer().getWorld().getSpawnLocation());
        //                    }
        //                }
        //            }.executeNow();
        //        }

        if (!PermissionChecker.isCrew(event.player)) {
            player.inventory.clear()
            CommandCraftventure.giveMenu(player)
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
            if (player.isConnected()) {
                MailManager.notifyUnreads(player.uniqueId)
            }
        }, (20 * 5 + 1).toLong())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleportMonitor(playerTeleportEvent: PlayerTeleportEvent) {
        val player = playerTeleportEvent.player
        player.fireTicks = 0
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerTeleport(playerTeleportEvent: PlayerTeleportEvent) {
        if (playerTeleportEvent.cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            playerTeleportEvent.isCancelled = true
            return
        }
        if (playerTeleportEvent.player.gameMode == GameMode.SPECTATOR && playerTeleportEvent.player.gameState()?.ride != null) {
            playerTeleportEvent.isCancelled = true
            return
        }
        //        if (playerTeleportEvent.getPlayer().getGameMode() == GameMode.SPECTATOR) {
        //            playerTeleportEvent.setCancelled(true);
        //            return;
        //        }
        //        BalloonManager.onTeleport(playerTeleportEvent.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (event.player.vehicle != null) {
            event.isCancelled = true
            return
        }
        if (!event.player.hasPermission("cvguard.manipulate." + getEntityTypeName(event.rightClicked)) &&
            !event.player.hasPermission(
                "cvguard.manipulate." + getEntityTypeName(event.rightClicked) + ".set." + getMaterialName(
                    event.playerItem.type
                )
            ) &&
            !event.player.hasPermission(
                "cvguard.manipulate." + getEntityTypeName(event.rightClicked) + ".get." + getMaterialName(
                    event.armorStandItem.type
                )
            )
        ) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug(
                    "PlayerArmorStandManipulateEvent cancelled due to missing permission for manipulating " + getEntityTypeName(
                        event.rightClicked
                    )
                )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDeath(event: EntityDeathEvent) {
        event.droppedExp = 0
        event.drops.clear()
        //        Logger.console("Entity death");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        //        event.setCancelled(true);
        if (event.cause == EntityDamageEvent.DamageCause.VOID || event.entity.location.y < 0) {
            return
        }
        val entity = event.entity

        if (entity is Hanging && event.cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.isCancelled = true
            return
        }

        if (entity is ArmorStand && event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.isCancelled = true
            return
        }

        if (entity is Player) {

            //            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            //                if (event.getDamage() > 0) {
            //                    // int duration, int amplifier, boolean ambient, boolean particles, Color color)
            //                    player.sendMessage(Translation.ROAMING_LEGS_HURT.getTranslation(player, (int) Math.ceil(event.getDamage() * 0.5)));
            //                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
            //                            (int) event.getDamage() * 10,
            //                            Math.min((int) (event.getDamage() / 4), 3),
            //                            true,
            //                            true,
            //                            true));
            //                }
            //            }

            if (event.cause == EntityDamageEvent.DamageCause.SUFFOCATION && entity.isInsideVehicle) {
                event.isCancelled = true
                return
            }
        } else {
            if (event.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || event.cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                if (entity is Hanging) {
                    //                Logger.info("EntityDamageEvent cancelled " + event.getCause().name());
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
//        logcat { "Entity ${event.entity.type} damaged by ${event.damager.type}" }
        if (event.damager is Firework) {
            val firework = event.damager as Firework
//            logcat { "Damager is firework by ${firework.shooter} damagerIsShooter=${event.damager !== firework.shooter}" }
            if (firework.shooter is Player && event.damager !== firework.shooter) {
//                logcat { "Disable player fire" }
                event.isCancelled = true
                return
            }
        }
//        Logger.info("EntityDamageByEntityEvent on " + event.damager.javaClass.simpleName + " by " + event.damager.javaClass.simpleName + " for " + event.cause + " for taker ${getEntityTypeName(event.entity)}")
        if (event.entity !is Player) {
            if (event.cause == EntityDamageEvent.DamageCause.FIRE ||
                event.cause == EntityDamageEvent.DamageCause.LIGHTNING ||
                event.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                //                    event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE ||
                event.cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
            ) {
                //                Logger.console("CANCELLED");
                return
            }
        }
        val taker = event.entity
        val damager = event.damager
        if (damager is Player) {
            val playerDamager = damager
            if (taker is ArmorStand && PermissionChecker.isCrew(damager) && !playerDamager.isInsideVehicle && playerDamager.isSneaking) {
                val itemStack = damager.inventory.itemInMainHand
                if (itemStack != null && itemStack.type != Material.ARMOR_STAND) {
                    val armorStand = taker
                    var name: String? =
                        ItemStackUtils2.getDisplayName(itemStack)
                    if (name != null) {
                        name = name.lowercase(Locale.getDefault())
                        if (name.contains("chest"))
                            armorStand.setChestplate(itemStack)
                        else if (name.contains("helmet"))
                            armorStand.setHelmet(itemStack)
                        else if (name.contains("boots"))
                            armorStand.setBoots(itemStack)
                        else if (name.contains("leggings"))
                            armorStand.setLeggings(itemStack)
                        else if (name.contains("off"))
                            armorStand.equipment.setItemInOffHand(itemStack)
                        else if (name.contains("hand"))
                            armorStand.setItemInHand(itemStack)
                        event.isCancelled = true
                        return
                    }
                }
            }
            val damageCauseName = getDamageCauseName(event.cause)
            if (!(damager.hasPermission("cvguard.damage.dealto.entity." + getEntityTypeName(taker) + "." + damageCauseName) ||
                        taker is Monster && damager.hasPermission("cvguard.damage.dealto.entity.monster.$damageCauseName") ||
                        taker is Vehicle && damager.hasPermission("cvguard.damage.dealto.entity.vehicle.$damageCauseName") ||
                        taker is Projectile && damager.hasPermission("cvguard.damage.dealto.entity.projectile.$damageCauseName") ||
                        taker is Hanging && damager.hasPermission("cvguard.damage.dealto.entity.hanging.$damageCauseName") ||
                        taker is Ambient && damager.hasPermission("cvguard.damage.dealto.entity.ambient.$damageCauseName") ||
                        taker is Flying && damager.hasPermission("cvguard.damage.dealto.entity.flying.$damageCauseName") ||
                        taker is Explosive && damager.hasPermission("cvguard.damage.dealto.entity.explosive.$damageCauseName") ||
                        taker is Animals && damager.hasPermission("cvguard.damage.dealto.entity.animal.$damageCauseName"))
            ) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug(
                        "EntityDamageByEntityEvent cancelled due to missing permission for dealing damage to " + getEntityTypeName(
                            event.entity
                        )
                    )
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerExpChangeEvent(event: PlayerExpChangeEvent) {
        event.amount = 0
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityBreakDoorEvent(event: EntityBreakDoorEvent) {
        if (event.entity !is Player)
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerBucketFillEvent(event: PlayerBucketFillEvent) {
        if (!event.player.hasPermission("cvguard.bucket.fill")) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("PlayerBucketFillEvent cancelled due to missing permission for bucket filling")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerBucketEmptyEvent(event: PlayerBucketEmptyEvent) {
        if (!event.player.hasPermission("cvguard.bucket.empty")) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("PlayerBucketEmptyEvent cancelled due to missing permission for bucket emptying")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerLeashEntityEvent(event: PlayerLeashEntityEvent) {
        if (!event.player.hasPermission("cvguard.interact.leash")) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("onPlayerLeashEntityEvent cancelled due to missing permission for leashing")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        val player = event.entity as? Player ?: return
        if (!player.hasPermission("craftventure.changeblock.${event.block.type.key.key}.to.${event.blockData.material.key.key}"))
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBurn(event: BlockBurnEvent) {
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
//        Logger.debug("Placing block ${event.block.type}")
        if (!event.player.hasPermission("cvguard.interact.place." + getMaterialName(event.blockPlaced.type))) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("BlockPlaceEvent cancelled due to missing PLACE permission for " + getMaterialName(event.blockPlaced.type))
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        if (!event.player.hasPermission("cvguard.interact.remove." + getMaterialName(event.block.type))) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("BlockBreakEvent cancelled due to missing REMOVE permission for " + getMaterialName(event.block.type))
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        if (event.foodLevel < 7) {
//            logcat { "Retaining level at 7 (${event.foodLevel})" }
            event.foodLevel = 7
        } else if (event.foodLevel == 19 && event.entity.foodLevel == 20) {
//            logcat { "Display message" }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        if (!event.player.hasPermission("cvguard.interact.entity." + getEntityTypeName(event.rightClicked))) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug(
                    "PlayerInteractEntityEvent cancelled due to missing permission for interacting with " + getEntityTypeName(
                        event.rightClicked
                    )
                )
        }

        val rightClicked = event.rightClicked as? ItemFrame ?: return

        val itemStack = rightClicked.item
        if (itemStack != null && itemStack.type == Material.FILLED_MAP && !event.player.isSneaking) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockCanBuild(event: BlockCanBuildEvent) {
        if (event.player == null) return
        if (!event.player!!.hasPermission("cvguard.build." + event.blockData.material.name)) {
            if (DEBUG)
                Logger.debug("BlockCanBuildEvent cancelled, %s can not build here", false, event.player!!.name)
            event.isBuildable = false
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.action == Action.LEFT_CLICK_AIR ||
            event.action == Action.RIGHT_CLICK_AIR ||
            event.clickedBlock == null
        ) {
            return
        }

        val player = event.player
        val block = event.clickedBlock
        val material = block?.type ?: return

        val state = block.state
        if (state is TileState) {
            if (state.persistentDataContainer.get(
                    CraftventureKeys.TILE_STATE_ALLOW_INTERACT,
                    PersistentDataType.STRING
                ) == "*"
            ) {
                // Block explicitly allows interaction
                return
            }
        }

        if (event.item?.type == Material.MILK_BUCKET) {
            event.setUseInteractedBlock(Event.Result.DENY)
            event.setUseItemInHand(Event.Result.ALLOW)
            return
        }

        if (material == Material.DRAGON_EGG) {
            if (!player.isSneaking || !PermissionChecker.isCrew(player)) {
                event.isCancelled = true
                if (PermissionChecker.isCrew(player))
                    player.sendMessage(CVTextColor.serverError + "You can only interact with dragoneggs while sneaking!")

                if (!player.isInsideVehicle) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 20 * 5, 1, true, false))
                    player.playSound(block.location, Sound.ENTITY_SKELETON_HORSE_DEATH, 1f, 0.1f)
                    //                    player.playSound(block.getLocation(), SoundUtils.DRAGON_EGG, 1f, 0.1f);
                    LookAtUtil.makePlayerLookAt(player, Vector(-48.5, 51.0, -439.5))
                    object : AsyncTask() {
                        override fun doInBackground() {
                            MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "the_call")
                        }
                    }.executeNow()
                }
            }
        }

        val materialName = getMaterialName(material)
        val itemStack = player.inventory.itemInMainHand
        val type = if (itemStack != null) itemStack.type else Material.AIR
        val itemInHandName = getMaterialName(type)
        if (event.action == Action.LEFT_CLICK_BLOCK) {
            if (!player.hasPermission("cvguard.interact.block.leftclick.with.$itemInHandName.on.$materialName")) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug("PlayerInteractEvent cancelled, missing LEFT_CLICK_BLOCK with $itemInHandName on $materialName")
                return
            }
        }
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            if (!player.hasPermission("cvguard.interact.block.rightclick.with.$itemInHandName.on.$materialName")) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug("PlayerInteractEvent cancelled, missing RIGHT_CLICK_BLOCK with $itemInHandName on $materialName")
                return
            }
        }
        if (event.action == Action.PHYSICAL) {
            if (!player.hasPermission("cvguard.interact.block.physical.with.$itemInHandName.on.$materialName")) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug("PlayerInteractEvent cancelled, missing PHYSICAL with $itemInHandName on $materialName")
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onVehicleEnterEvent(event: VehicleEnterEvent) {
        if (event.entered is Player) {
            val player = event.entered as Player
            if (player.isInsideVehicle && player.vehicle?.entityId != event.vehicle.entityId) {
                //                Logger.console("Cancel enter, already in vehicle");
                event.isCancelled = true
                return
            }
            if (!player.hasPermission("cvguard.interact.vehicle.enter." + getEntityTypeName(event.vehicle))) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug(
                        "VehicleEnterEvent cancelled due to missing permission for entering " + getEntityTypeName(
                            event.vehicle
                        )
                    )
            }
        }
    }

    //    @EventHandler(priority = EventPriority.LOWEST)
    //    public void onVehicleExitEvent(VehicleExitEvent event) {
    //        if (event.getExited() instanceof Player) {
    //            Player player = (Player) event.getExited();
    //            if (!player.hasPermission("cvguard.interact.vehicle.exit." + getEntityTypeName(event.getVehicle()))) {
    //                event.setCancelled(true);
    //                if (DEBUG)
    //                    Logger.console("VehicleEnterEvent cancelled due to missing permission for exiting " + getEntityTypeName(event.getVehicle()));
    //            }
    //        }
    //    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun VehicleDestroyEvent(event: VehicleDestroyEvent) {
        if (event.attacker is Player) {
            val player = event.attacker as Player
            if (!player.hasPermission("cvguard.damage.destroy." + getEntityTypeName(event.vehicle))) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug(
                        "VehicleDestroyEvent cancelled due to missing permission for exiting " + getEntityTypeName(
                            event.vehicle
                        )
                    )
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun VehicleDamageEvent(event: VehicleDamageEvent) {
        if (event.attacker is Player) {
            val player = event.attacker as Player
            if (!player.hasPermission("cvguard.damage.attack." + getEntityTypeName(event.vehicle))) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug(
                        "VehicleDamageEvent cancelled due to missing permission for exiting " + getEntityTypeName(
                            event.vehicle
                        )
                    )
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockSpreadEvent(event: BlockSpreadEvent) {
        if (event.block.type == Material.FIRE ||
            event.block.type == Material.LAVA ||
            event.source.type == Material.FIRE ||
            event.source.type == Material.LAVA
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityExplodeEvent(event: EntityExplodeEvent) {
        event.yield = 0f
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onEntityExplodeEventMonitor(event: EntityExplodeEvent) {
        if (event.isCancelled) {
            val location = event.location
            location.spawnParticleX(Particle.EXPLOSION_LARGE, 8, 1.5, 1.5, 1.5)
            location.world.playSound(
                location, Sound.ENTITY_GENERIC_EXPLODE, 4f,
                (1.0f + (CraftventureCore.getRandom().nextFloat() - CraftventureCore.getRandom()
                    .nextFloat()) * 0.2f) * 0.7f
            )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockExplodeEvent(event: BlockExplodeEvent) {
        event.yield = 0f
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onBlockExplodeEventMonitor(event: BlockExplodeEvent) {
        if (event.isCancelled) {
            val location = event.block.location.toCenterLocation()
            location.spawnParticleX(Particle.EXPLOSION_LARGE, 8, 1.5, 1.5, 1.5)
            location.world.playSound(
                location, Sound.ENTITY_GENERIC_EXPLODE, 4f,
                (1.0f + (CraftventureCore.getRandom().nextFloat() - CraftventureCore.getRandom()
                    .nextFloat()) * 0.2f) * 0.7f
            )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onStructureGrowEvent(event: StructureGrowEvent) {
        if (event.player == null || !event.isFromBonemeal) {
            event.isCancelled = true
            return
        }
        if (!event.player!!.hasPermission("cvguard.interact.grow.bonemeal") && event.isFromBonemeal) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("StructureGrowEvent cancelled due to missing permission for bonemeal usage")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerFishEvent(event: PlayerFishEvent) {
        if (!event.player.hasPermission("cvguard.interact.fish")) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("PlayerFishEvent cancelled due to missing permission for fishing")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerEditBookEvent(event: PlayerEditBookEvent) {
        if (!event.player.hasPermission("cvguard.interact.book.edit")) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("PlayerEditBookEvent cancelled due to missing permission for book editing")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onHangingBreakByEntityEvent(event: HangingBreakByEntityEvent) {
//        Logger.debug("HangingBreakByEntityEvent ${event.cause} > ${event.remover}");
        val player = event.remover as? Player
        if (player != null) {
            if (!player.hasPermission("cvguard.entity.remove." + getEntityTypeName(event.entity))) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug(
                        "HangingBreakByEntityEvent cancelled due to missing permission for removing " + getEntityTypeName(
                            event.entity
                        )
                    )
                return
            }
//            val itemInHand = player.inventory.itemInMainHand
//            val type = itemInHand.type
//            if (!type.isSign() && type != Material.PAINTING && type != Material.ITEM_FRAME) {
//                player.sendMessage(CVTextColor.serverError + "You can only remove this entity when holding a painting or itemframe!")
//                event.isCancelled = true
//            }
        } else {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onHangingPlaceEvent(event: HangingPlaceEvent) {
        if (!event.player!!.hasPermission("cvguard.entity.spawn." + getEntityTypeName(event.entity))) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug(
                    "HangingPlaceEvent cancelled due to missing permission for spawning " + getEntityTypeName(
                        event.entity
                    )
                )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onHangingBreakEvent(event: HangingBreakEvent) {
//        Logger.debug("HangingBreakEvent ${event.cause} /tp ${event.entity.location.blockX} ${event.entity.location.blockY} ${event.entity.location.blockZ}")
        if (event.cause != HangingBreakEvent.RemoveCause.ENTITY) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerEggThrowEvent(event: PlayerEggThrowEvent) {
        if (!PermissionChecker.isCrew(event.player)) {
            event.egg.remove()
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerItemConsumeEvent(event: PlayerItemConsumeEvent) {
        if (!event.player.hasPermission("cvguard.consume." + getMaterialName(event.item.type))) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug(
                    "PlayerItemConsumeEvent cancelled due to missing permission for consuming " + getMaterialName(
                        event.item.type
                    )
                )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerBedEnterEvent(event: PlayerBedEnterEvent) {
        if (!event.player.hasPermission("cvguard.interact.bed.enter")) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug("PlayerBedEnterEvent cancelled due to missing permission for entering bed")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDropItemEvent(event: PlayerDropItemEvent) {
        if (!event.player.hasPermission("cvguard.item.drop." + getMaterialName(event.itemDrop.itemStack.type))) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug(
                    "PlayerDropItemEvent cancelled due to missing permission for item drop " + getMaterialName(
                        event.itemDrop.itemStack.type
                    )
                )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityPickupItemEvent(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (!player.hasPermission("cvguard.item.pickup." + getMaterialName(event.item.itemStack.type))) {
            event.isCancelled = true
            //            if (DEBUG)
            //                Logger.console("PlayerPickupItemEvent cancelled due to missing permission for picking up " + getMaterialName(event.getItem().getItemStack().getActionTypeId()));
        }

        if (!PermissionChecker.isCrew(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerShearEntityEvent(event: PlayerShearEntityEvent) {
        if (!event.player.hasPermission("cvguard.shear." + getEntityTypeName(event.entity))) {
            event.isCancelled = true
            if (DEBUG)
                Logger.debug(
                    "PlayerShearEntityEvent cancelled due to missing permission for shearing " + getEntityTypeName(
                        event.entity
                    )
                )
        }
    }

    //    @EventHandler(priority =EventPriority.LOWEST)
    //    public void onPlayerAchievementAwardedEvent(PlayerAchievementAwardedEvent event) {
    //        event.setCancelled(true);
    //    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPortalCreate(event: PortalCreateEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            if (!player.hasPermission("cvguard.portal.create")) {
                event.isCancelled = true
                if (DEBUG)
                    Logger.debug("EntityCreatePortalEvent cancelled due to missing permission for creating portals")
            }
        } else {
            event.isCancelled = true
        }
    }

    override fun onPluginMessageReceived(channel: String, player: Player, data: ByteArray) {
        if (channel.startsWith("wdl")) {
            //                Logger.debug(CVChatColor.ERROR + player.getName() + " kicked for using WorldDownloader", true);
            executeSync((20 * 5).toLong()) {
                player.sendMessage(CVTextColor.serverError + "Welcome to the club of World Downloaders. It's okay, we've all been there. It's okay to us as long as you intend to download it for private use only. Maybe your just an explorer trying to uncover the latest secrets of Craftventure. Cool. Maybe you're trying to sell our map to other folks, which we're not okay with, we don't know. Private use only!")
            }
            executeAsync {
                MainRepositoryProvider.playerKeyValueRepository.createOrUpdate(
                    player.uniqueId,
                    "worlddownloader",
                    java.lang.Long.toString(System.currentTimeMillis())
                )
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCauldronLevelChange(event: CauldronLevelChangeEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            //            Logger.info(String.format("Cauldron event %s", event.getReason().name()));
            if (!player.hasPermission("cvguard.cauldron.change." + event.reason.name)) {
                //                Logger.info("Cancel cauldron");
                event.isCancelled = true
                player.fireTicks = 0
            }
        }
    }

    @EventHandler
    fun onFeatureToggled(event: FeatureToggledEvent) {
        when (event.feature) {
            FeatureManager.Feature.VIEW_OTHER_PLAYERS -> VisibilityManager.updateAll()
            else -> {}
        }
    }

    companion object {
        private val DEBUG = false
    }
}
