package net.craftventure.audioserver.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.audioserver.packet.PacketAreaData
import net.craftventure.audioserver.packet.PacketAreaState
import net.craftventure.audioserver.packet.PacketSync
import net.craftventure.audioserver.websocket.AudioServerHandler
import net.craftventure.audioserver.websocket.ChannelMetaData
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.TimeUtils
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

@JsonClass(generateAdapter = true)
data class AudioArea(
    val name: String? = null,
    @Json(name = "display_name")
    val displayName: String? = null,
    @Json(name = "area_type")
    val areaType: AreaType? = null,
    @Json(name = "player_state")
    val playerState: PlayerState = PlayerState.ALWAYS,
    @Json(name = "repeat_type")
    var repeatType: RepeatType? = RepeatType.REPEAT_SEQUENCE,
    @Json(name = "enabled")
    var isEnabled: Boolean = false,
    @Json(name = "fade_time")
    val fadeTime: Long = 0,
    val duration: Long = 0,
    val volume: Double = 0.toDouble(),
    val overrides: List<String> = LinkedList(),
    val resources: List<Resource> = LinkedList(),
    val filters: List<AreaFilter> = LinkedList(),
    @Json(name = "area_part_names")
    val areaPartNames: List<String> = LinkedList()
) {
    @Transient
    val areaId = id++

    @Transient
    var loadTime = System.currentTimeMillis()
        private set

    @Transient
    val players = ArrayList<Player>()

    @Transient
    val areaParts = ArrayList<AreaPart>()

    fun addAreaPart(areaPart: AreaPart) {
        //        Logger.console("Adding AreaPart " + areaPart.getName() + " to " + name);
        areaParts.add(areaPart)
    }

    operator fun contains(player: Player): Boolean {
        return players.contains(player)
    }

    fun validate() {
        if (repeatType == null) {
            repeatType = RepeatType.REPEAT_SEQUENCE
            logcat(logToCrew = true) { "RepeatType for $name was invalid and was changed to REPEAT_SEQUENCE" }
        }
        resources.forEachIndexed { index, resource ->
            if ((resource.artist == null || resource.artist.equals(
                    "Unknown",
                    ignoreCase = true
                )) && resource.name == null
            ) {
                logcat(logToCrew = true) { "No artist set for resource #${index + 1} at area $name" }
            }
        }
//        if (filters.isNotEmpty()) {
//            logcat(logToCrew = true) { "Area $name uses filters which may not work properly at all browsers at the moment as I (Joey) still need to fix it to modern standards one day" }
//        }
    }

    fun handleJoin(player: Player) {
//        Logger.debug("Handling join of ${player.name} for $areaId ${contains(player)}")
        if (contains(player)) {
            sendAreaDefinition(player)
            sendAreaState(player)
        }
    }

    protected fun isInArea(player: Player): Boolean {
        if (!isEnabled) {
            //            Logger.console("Not enabled");
            return false
        }

        var inArea = false
        for (i in areaParts.indices) {
            val areaPart = areaParts[i]
            if (areaPart.isInAreaPart(player)) {
                inArea = true
                break
            }
        }
        //        Logger.console("In AudioArea? " + inArea);
        if (inArea) {
            if (playerState === PlayerState.IN_VEHICLE && !player.isInsideVehicle) {
                //                Logger.console("Vehicling");
                return false
            } else if (playerState === PlayerState.SPECTATING && player.spectatorTarget == null) {
                return false
            } else if (playerState === PlayerState.WALKING && player.isInsideVehicle) {
                //                Logger.console("Walking");
                return false
            }
            return true
        }
        //        Logger.console("Default false");
        return false
    }

    @JvmOverloads
    fun sendAreaState(player: Player?, playing: Boolean = players.contains(player)): Boolean {
        if (player != null) {
            val channelMetaData = AudioServerHandler.getChannel(player)
            if (channelMetaData != null) {
                PacketAreaState(areaId, playing).send(channelMetaData)
                return true
            }
        }
        return false
    }

    fun sendAreaDefinition(player: Player?): Boolean {
//        Logger.debug("Sending area ${areaId} to ${player?.name}...");
        if (player != null) {
            val channelMetaData = AudioServerHandler.getChannel(player)
            if (channelMetaData != null) {
                if (!channelMetaData.hasSentArea(areaId)) {
                    channelMetaData.addSentArea(areaId)
                    PacketAreaData(this).send(channelMetaData)
                    return true
                }
            }
        }
        return false
    }

    fun update() {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.AUDIOSERVER_UPDATING)) return
        for (player in Bukkit.getOnlinePlayers()) {
            val inArea = isInArea(player)
            if (inArea && areaType !== AreaType.RIDE) {
                if (!players.contains(player)) {
                    addPlayer(player)
                }
            } else if (!inArea) {
                if (players.contains(player)) {
                    removePlayer(player)
                }
            }
        }
    }

    fun sendDisplayEnterMessage(player: Player?, channelMetaData: ChannelMetaData?) {
        if (player == null)
            return

        if (channelMetaData != null) {
            val lastSentAreaTitle = channelMetaData.lastSentAreaTitle
            val areTitlesSame =
                lastSentAreaTitle == null && displayName == null || lastSentAreaTitle != null && lastSentAreaTitle == displayName
            channelMetaData.lastSentAreaTitle = displayName
            if (areTitlesSame) {
                //                Logger.console("Ignoring title cause the last one was already " + displayName);
                return
            }
        }

        if (displayName != null) {
            MessageBarManager.display(
                player,
                Component.text(displayName, CVTextColor.serverNotice),
                MessageBarManager.Type.AUDIOSERVER_AREA,
                TimeUtils.secondsFromNow(4.0),
                ChatUtils.ID_AUDIOSERVER_AREA_NAME
            )
        }
    }

    fun addPlayer(player: Player) {
        players.add(player)
        //        Logger.console("Adding " + player.getName() + " to " + name);
        val channelMetaData = AudioServerHandler.getChannel(player)
        if (!sendAreaDefinition(player)) {
            if (channelMetaData != null) {
                sync(player)
            }
        }
        // Just send the area titles the old way when people are not connected to the AudioServer
        if (channelMetaData == null) {
            sendDisplayEnterMessage(player, null)
        }
        sendAreaState(player, true)
    }

    fun removePlayer(player: Player) {
        players.remove(player)
        //        Logger.console("Removing " + player.getName() + " from " + name);
        sendAreaState(player, false)
    }

    fun sync(loadTime: Long) {
        this.loadTime = loadTime
        for (i in players.indices) {
            val player = players[i]
            val channelMetaData = AudioServerHandler.getChannel(player)
            if (channelMetaData != null) {
                PacketSync(areaId, loadTime).send(channelMetaData)
            }
        }
    }

    fun sync(player: Player): Boolean {
        val channelMetaData = AudioServerHandler.getChannel(player)
        if (channelMetaData != null) {
            PacketSync(areaId, loadTime).send(channelMetaData)
            return true
        }
        return false
    }

    fun sync(player: Player, time: Long): Boolean {
        val channelMetaData = AudioServerHandler.getChannel(player)
        if (channelMetaData != null) {
            PacketSync(areaId, time).send(channelMetaData)
            return true
        }
        return false
    }

    fun getAreaParts(): List<AreaPart> {
        return areaParts
    }

    companion object {
        private var id: Long = 1
    }
}
