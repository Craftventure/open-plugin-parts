package net.craftventure.audioserver.listener

import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.event.AudioServerConnectedEvent
import net.craftventure.audioserver.packet.*
import net.craftventure.audioserver.websocket.AudioServerHandler
import net.craftventure.bukkit.ktx.event.AsyncPlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.manager.WorldBorderManager
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.coroutine.executeAsync
import net.craftventure.core.ktx.json.toJson
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.isAllowed
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class MainListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        Bukkit.getScheduler().scheduleSyncDelayedTask(PluginProvider.getInstance(), {
            executeAsync {
                AudioServer.instance.sendAudioLink(player, false)
            }
        }, (20 * 10).toLong())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAudioConnect(audioServerConnectedEvent: AudioServerConnectedEvent) {
        updateAudioAreas(audioServerConnectedEvent.player.location, audioServerConnectedEvent.player, false)
        val to = audioServerConnectedEvent.player.location
        PacketLocationUpdate(to.x, to.y, to.z, to.yaw, to.pitch).send(audioServerConnectedEvent.channelMetaData)

        audioServerConnectedEvent.channelMetaData.send(
            PacketMapLayersAdd.createInitialPacket(AudioServer.instance.audioServerConfig!!.overlays).toJson()
        )
        audioServerConnectedEvent.channelMetaData.send(
            PacketMarkerAdd.createInitialPacket(AudioServer.instance.audioServerConfig!!.markers).toJson()
        )
        val markers = mutableListOf<PacketMarkerAdd.MapMarker>()
        MainRepositoryProvider.storeRepository.cachedItems.forEach { store ->
            val warp =
                store.warpId?.let { MainRepositoryProvider.warpRepository.findCachedByName(it) } ?: return@forEach
            markers.add(
                PacketMarkerAdd.MapMarker(
                    id = "shop_${store.id}",
                    popupName = store.displayName?.let { "Store: $it" },
                    group = "shop",
                    x = warp.x!!,
                    z = warp.z!!,
                    zIndex = warp.y!!.toInt(),
                    url = "\uD83D\uDECD",
                    type = "twemoji",
                    size = AudioPacketPoint(32.0, 32.0),
                    anchor = AudioPacketPoint(16.0, 16.0),
                    popupAnchor = AudioPacketPoint(0.0, -16.0),
                )
            )
        }

        MainRepositoryProvider.rideRepository.cachedItems.filter { it.state != null && it.state!!.isOpen }
            .forEach { ride ->
                val warp =
                    ride.warpId?.let { MainRepositoryProvider.warpRepository.findCachedByName(it) } ?: return@forEach
                if (!warp.isAllowed(audioServerConnectedEvent.player)) return@forEach
                markers.add(
                    PacketMarkerAdd.MapMarker(
                        id = "ride_${ride.id}",
                        popupName = ride.displayName?.let { "Ride: $it" },
                        group = "ride",
                        x = warp.x!!,
                        z = warp.z!!,
                        zIndex = warp.y!!.toInt(),
                        url = if (ride.name == "caroussel") "\uD83C\uDFA0" else when (ride.type) {
//                        RideType.COASTER -> "\uD83C\uDFA2"
//                        RideType.FLATRIDE -> "\uD83C\uDFA1"
                            else -> "\uD83C\uDF9F"
                        },
                        type = "twemoji",
                        size = AudioPacketPoint(32.0, 32.0),
                        anchor = AudioPacketPoint(16.0, 16.0),
                        popupAnchor = AudioPacketPoint(0.0, -16.0),
                    )
                )
            }
        audioServerConnectedEvent.channelMetaData.send(
            PacketMarkerAdd(
                PacketMarkerAdd.Mode.SET,
                "shop",
                markers.toSet()
            ).toJson()
        )

        if (audioServerConnectedEvent.player.isCrew()) {
            val borderAreas = WorldBorderManager.borderParts.sortedByDescending { it.crewOnly }
                .mapIndexed { index, borderConfig ->
                    PacketPolygonOverlayAdd.RectangleOverlay(borderConfig.area.min, borderConfig.area.max).apply {
                        id = "crew_worldborder_area_$index"
                        group = "crew_worldborder"
                        title = "World Border (${if (borderConfig.crewOnly) "crew only" else "guests"})"
                        fillColor = if (borderConfig.crewOnly) "#ffaa00" else "#555555"
                    }
                }
            audioServerConnectedEvent.channelMetaData.send(
                PacketPolygonOverlayAdd(
                    PacketPolygonOverlayAdd.Mode.SET,
                    "crew_worldborder",
                    borderAreas.toSet()
                ).toJson()
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerKick(event: PlayerKickEvent) {
        cleanPlayer(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cleanPlayer(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: AsyncPlayerLocationChangedEvent) {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.AUDIOSERVER_TRACKING)) return
        try {
            if (event.locationChanged)
                updateAudioAreas(event.to, event.player, false)
            updatePlayerLocation(event.to, event.player)
        } catch (e: Exception) {
            Logger.capture(e)
        }
    }

    fun cleanPlayer(player: Player) {
        AudioServer.instance.audioServer?.disconnect(player, "Disconnected from Craftventure")
    }

    fun updatePlayerLocation(to: Location, player: Player) {
        val metaData = AudioServerHandler.getChannel(player)
        if (metaData != null) {
            PacketLocationUpdate(to.x, to.y, to.z, to.yaw, to.pitch).send(metaData)
        }
    }

    fun updateAudioAreas(to: Location, player: Player, removeInstant: Boolean) {
        try {
            if (AudioServer.instance.audioServerConfig != null) {
                val areaParts = AudioServer.instance.audioServerConfig!!.areaParts
                for (i in areaParts.indices) {
                    val areaPart = areaParts[i]
                    areaPart.update(player, to)
                }
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }
    }
}
