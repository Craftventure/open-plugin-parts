package net.craftventure.core.protocol

import com.comphenix.packetwrapper.WrapperPlayClientChat
import com.comphenix.packetwrapper.WrapperPlayClientSettings
import com.comphenix.packetwrapper.WrapperPlayServerAttachEntity
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.injector.temporary.TemporaryPlayer
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.metadata.ClientSettingsMetadata
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.metadata.KeyValueMeta
import net.craftventure.core.npc.tracker.EntitySpawnTrackerManager
import net.craftventure.core.serverevent.PacketPlayerSteerEvent
import net.craftventure.core.serverevent.PacketUseEntityEvent
import net.craftventure.core.utils.CommandUtil
import net.craftventure.database.repository.PlayerKeyValueRepository
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player


object ProtocolHandler {
    private val leashList = ArrayList<ProtocolLeash>()

    private var hasInitialised = false

    fun init() {
        if (hasInitialised)
            return
        hasInitialised = true

        ProtocolLibrary.getProtocolManager().addPacketListener(object : PacketAdapter(
            CraftventureCore.getInstance(),
            PacketType.Play.Client.STEER_VEHICLE,
            PacketType.Play.Client.USE_ENTITY,
            PacketType.Play.Client.CHAT,
            PacketType.Play.Client.SETTINGS,
//            PacketType.Play.Client.LOOK,
//            PacketType.Play.Client.CUSTOM_PAYLOAD,

            // Fake leashes
//                PacketType.Play.Server.CUSTOM_PAYLOAD,
//            PacketType.Play.Server.PLAYER_INFO,
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.NAMED_ENTITY_SPAWN,
            PacketType.Play.Server.ENTITY_DESTROY,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
        ) {
            override fun onPacketReceiving(packetEvent: PacketEvent) {
                if (packetEvent.isCancelled) return
                try {
                    val player = packetEvent.player
                    if (player is TemporaryPlayer)
                        return

                    val start = System.currentTimeMillis()

                    if (packetEvent.packetType === PacketType.Play.Client.CHAT) {
                        val chat = WrapperPlayClientChat(packetEvent.packet)
                        val message: String? = chat.message
                        if (message != null && !message.startsWith("/")) {
                            if (PermissionChecker.isCrew(player)) {
                                val meta = KeyValueMeta.get(player)
                                if (meta != null) {
                                    if (meta.getKeyValue(PlayerKeyValueRepository.KEY_ADMIN_CHAT) != null) {
                                        packetEvent.isCancelled = true
                                        CommandUtil.sendToBungee(player, "ad $message")
                                        return
                                    }
                                }
                            }
                        }
//                    } else if (packetEvent.packetType === PacketType.Play.Client.LOOK) {
//                        val packet =packetEvent.packet.handle as ServerboundMovePlayerPacket
//                        logcat { "Look changed" }
                    } else if (packetEvent.packetType === PacketType.Play.Client.SETTINGS) {
                        val wrapperPlayClientSettings = WrapperPlayClientSettings(packetEvent.packet)
                        //                        Logger.info("Receiving client settings for %s", false, packetEvent.getPlayer().getName());
                        val clientSettingsMetadata = player.getOrCreateMetadata { ClientSettingsMetadata(player) }
                        clientSettingsMetadata.update(wrapperPlayClientSettings)
                    } else if (packetEvent.packetType === PacketType.Play.Client.USE_ENTITY) {
//                        Logger.debug("Packet ${packetEvent.packet.handle.javaClass.name}")
                        val useEntity = packetEvent.packet.handle as ServerboundInteractPacket
                        // https://nms.screamingsandals.org/1.20.1/net/minecraft/network/protocol/game/ServerboundInteractPacket.html > action
//                        val useEntity = WrapperPlayClientUseEntity(packetEvent.packet)
                        val bukkitEvent = PacketUseEntityEvent(
                            player,
                            useEntity.entityId,
                            PacketUseEntityEvent.Type.entries[packetEvent.packet.enumEntityUseActions.read(0).action.ordinal],
                        )
//                        Logger.debug("${bukkitEvent.type}")
                        Bukkit.getServer().pluginManager.callEvent(bukkitEvent)
                        if (bukkitEvent.isCancelled) {
                            packetEvent.isCancelled = true
                            WrapperPlayServerAttachEntity().apply {
                                this.entityID = useEntity.entityId
                                this.vehicleId = -1
                                try {
                                    sendPacket(player)
                                } catch (e: Exception) {
                                }
                            }
//                            Logger.info("Cancel entity use of type ${useEntity.type}")
                            //                        Logger.console("Called USE_ENTITY");
                        }
                    } else if (packetEvent.packetType === PacketType.Play.Client.STEER_VEHICLE) {
                        val vehicle = player.vehicle ?: return
                        val steerVehicle = packetEvent.packet.handle as ServerboundPlayerInputPacket
//                        Logger.debug("Handling steer unmount=${steerVehicle.isUnmount}")
                        //                    System.out.println(String.format("%s %s %f %f", steerVehicle.isUnmount(), steerVehicle.isJump(), steerVehicle.getForward(), steerVehicle.getSideways()));
                        val packetPlayerSteerEvent = PacketPlayerSteerEvent(
                            player,
                            vehicle,
                            steerVehicle.xxa,
                            steerVehicle.zza,
                            steerVehicle.isJumping,
                            steerVehicle.isShiftKeyDown
                        )
                        Bukkit.getServer().pluginManager.callEvent(packetPlayerSteerEvent)
                        if (packetPlayerSteerEvent.isCancelled) {
                            packetEvent.isCancelled = true
//                            Logger.debug("Cancelled STEER_VEHICLE");
                        }
                        //                    if (packetEvent.isCancelled())
                        //                    Logger.console("Cancelled? " + packetEvent.isCancelled());
                    }

                    val time = System.currentTimeMillis() - start
                    if (time > 2) {
                        val message = String.format(
                            "Protocol receiving took %dms for %s for %s",
                            time,
                            packetEvent.packetType.name(),
                            player.name
                        )
                        Logger.warn(message)
                        //                    Logger.capture(new TimeLimitExceededException(message));
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onPacketSending(packetEvent: PacketEvent) {
                if (packetEvent.isCancelled) return
                try {
                    val player = packetEvent.player
                    if (player is TemporaryPlayer)
                        return

                    val start = System.currentTimeMillis()

                    /*if (packetEvent.packetType === PacketType.Play.Server.PLAYER_INFO) {
                        if (DateUtils.isAprilFools) {
                            val packet = WrapperPlayServerPlayerInfo(packetEvent.packet)

                            packet.data.forEach { infoData ->
                                val infoPlayer = Bukkit.getPlayer(infoData.profile.uuid)
                                val metadata = infoPlayer?.getOrCreateMetadata {
                                    GameProfileMetadata(player,
                                        MainRepositoryProvider.cachedGameProfileRepository.cachedItems
                                            .filter { it.id!!.startsWith("cv_") }
                                            .random())
                                } ?: return@forEach
//                                infoData.profile.properties?.forEach { s, wrappedSignedProperty ->
//                                    Logger.debug("1 $s -> ${wrappedSignedProperty.name}/${wrappedSignedProperty.signature}/${wrappedSignedProperty.value}")
//                                }
                                infoData.profile.properties.removeAll("textures")
                                infoData.profile.properties?.put(
                                    "textures",
                                    WrappedSignedProperty(
                                        "textures",
                                        metadata.gameProfile.value,
                                        metadata.gameProfile.signature
                                    )
                                )
//                                infoData.profile.properties?.forEach { s, wrappedSignedProperty ->
//                                    Logger.debug("2 $s -> ${wrappedSignedProperty.name}/${wrappedSignedProperty.signature}/${wrappedSignedProperty.value}")
//                                }
                            }
                        }
                    } else */if (packetEvent.packetType === PacketType.Play.Server.ENTITY_DESTROY) {
                        val wrapperPlayServerEntityDestroy =
                            packetEvent.packet.handle as ClientboundRemoveEntitiesPacket
                        val entityIDs = wrapperPlayServerEntityDestroy.entityIds
                        for (i in entityIDs.indices) {
                            val id = entityIDs[i]
                            onEntityDestroyTo(id, player)
                        }
                    } else if (packetEvent.packetType === PacketType.Play.Server.SPAWN_ENTITY) {
                        val cancel = onEntitySpawnTo(packetEvent.packet.integers.read(0), player)
                        if (cancel) {
                            packetEvent.isCancelled = true
                            //                        return;
                        }
                    } else if (packetEvent.packetType === PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                        onEntitySpawnTo(packetEvent.packet.integers.read(0), player)
                    }

                    val time = System.currentTimeMillis() - start
                    if (time > 2) {
                        val message =
                            String.format("Protocol sending took %dms for %s", time, packetEvent.packetType.name())
                        Logger.warn(message)
                        //                    Logger.capture(new TimeLimitExceededException(message));
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        })
    }

    fun destroy() {
        for (protocolLeash in ArrayList(leashList)) {
            protocolLeash.destroy()
        }
    }

    fun addLeash(leash: ProtocolLeash) {
        synchronized(leashList) {
            if (!leashList.contains(leash))
                leashList.add(leash)
        }
    }

    fun removeLeash(leash: ProtocolLeash) {
        leashList.remove(leash)
    }

    private fun onEntityDestroyTo(destroyedEntityId: Int, player: Player) {
        EntitySpawnTrackerManager.onEntityDestroyTo(destroyedEntityId, player)
    }

    private fun onEntitySpawnTo(spawnedEntityId: Int, player: Player): Boolean {
        val spawning = Bukkit.getOnlinePlayers().firstOrNull { it.entityId == spawnedEntityId }
        if (spawning != null) {
//            logcat { "Spawning ${spawning.name} to ${player.name}" }
            executeSync { spawning.equippedItemsMeta()?.applySpawnPacketsTo(player) }
        }
        EntitySpawnTrackerManager.onEntitySpawnTo(spawnedEntityId, player)
        synchronized(leashList) {
            for (i in leashList.indices) {
                val protocolLeash = leashList[i]
                if (protocolLeash.`is`(spawnedEntityId)) {
                    Bukkit.getScheduler()
                        .scheduleSyncDelayedTask(CraftventureCore.getInstance()) { protocolLeash.spawn(player) }
                }
            }
        }
        return false
    }
}
