package net.craftventure.audioserver.spatial

import net.craftventure.audioserver.extensions.getAudioChannelMeta
import net.craftventure.audioserver.packet.BasePacket
import net.craftventure.audioserver.packet.PacketSpatialAudioDefinition
import net.craftventure.audioserver.packet.PacketSpatialAudioRemove
import net.craftventure.audioserver.packet.PacketSpatialAudioUpdate
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.core.ktx.extension.equalsWithPrecision
import net.craftventure.core.ktx.json.toJson
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin

class SpatialAudio(
    initialLocation: Location,
    val soundUrl: String,
    distanceModel: PacketSpatialAudioDefinition.DistanceModel = PacketSpatialAudioDefinition.DistanceModel.inverse,
    panningModel: PacketSpatialAudioDefinition.PanningModel = PacketSpatialAudioDefinition.PanningModel.HRTF,
    var distance: Double = 30.0,
    fadeOutStartDistance: Double = distance * 0.7,
    fadeOutEndDistance: Double = distance
) {
    private var location = initialLocation
    var started = false
        private set

    private val id = (SpatialAudio.id.incrementAndGet()).toString()
    private val players = HashSet<Player>()
    private val packetUpdate = PacketSpatialAudioUpdate(
        audioId = id,
        x = location.x,
        y = location.y,
        z = location.z,
        fadeOutStartDistance = fadeOutStartDistance,
        fadeOutEndDistance = fadeOutEndDistance,
        playing = true,
        volume = 0.0
    )
    private val packetDefinition = PacketSpatialAudioDefinition(
        audioId = id,
        soundUrl = soundUrl,
        distanceModel = distanceModel,
        panningModel = panningModel,
        state = packetUpdate
    )
    private val packetRemoval = PacketSpatialAudioRemove(audioId = id)
    private var deltaUpdatePacket: PacketSpatialAudioUpdate? = null

    private var isRunningBatch = false
    private fun startDeltaUpdate(action: PacketSpatialAudioUpdate.() -> Unit) {
        if (deltaUpdatePacket == null)
            deltaUpdatePacket = PacketSpatialAudioUpdate(audioId = id)
        return action(deltaUpdatePacket!!)
    }

    private fun sendDeltaUpdate() {
        if (isRunningBatch) return

        if (deltaUpdatePacket != null) {
            sendPacket(deltaUpdatePacket!!)
            deltaUpdatePacket = null
        }
    }

    private fun recheckPlayers() {
        Bukkit.getOnlinePlayers().forEach {
            updateFor(it)
        }
    }

    fun start() {
        if (started) return
        SpatialAudioManager.instance.register(this)
        started = true
        recheckPlayers()
    }

    fun stop() {
        if (!started) return
        SpatialAudioManager.instance.unregister(this)
        players.toList().forEach {
            removePlayer(it)
        }
        players.clear()
        started = false
    }

    fun pause(): Boolean {
        if (!started) return false
        started = false
        if (DEBUG_CHANGES)
            Logger.debug("pause $id $soundUrl")
        SpatialAudioManager.instance.unregister(this)
        val batch = isRunningBatch
        isRunningBatch = false
        playing(false)
        isRunningBatch = batch
        players.clear()
        return true
    }

    fun resume(): Boolean {
        if (started) return false
        started = true
        if (DEBUG_CHANGES)
            Logger.debug("resume $id $soundUrl")
        val batch = isRunningBatch
        isRunningBatch = false
        playing(true)
        isRunningBatch = batch
        SpatialAudioManager.instance.register(this)
        recheckPlayers()
        return true
    }

    private fun addPlayerIfConnected(player: Player) {
//        Logger.debug("Trying to add")
        val meta = player.getAudioChannelMeta()
        if (meta != null) {
            if (players.add(player)) {
                if (DEBUG_CHANGES)
                    Logger.debug("${player.name} added to $id ($soundUrl)")

//                Logger.debug("${player.name} ${packetDefinition.toJson()}")
                packetDefinition.send(meta)
            }
        }
    }

    private fun removePlayer(player: Player) {
//        Logger.debug("Trying to remove ${player.name}")
        if (players.remove(player)) {
            if (DEBUG_CHANGES)
                Logger.debug("${player.name} remove from $id")
            val meta = player.getAudioChannelMeta()
            if (meta != null) {
//                Logger.debug("${player.name} ${packetRemoval.toJson()}")
                packetRemoval.send(meta)
            }
        }
    }

    fun updateFor(player: Player, location: Location = player.location) {
        if (!started) return

        val isNear = this.location.distanceSquared(location) < distance * distance
        val isConnected = player.isConnected()
        val isAudioConnected = player.getAudioChannelMeta() != null
        val isAdded = players.contains(player)
//        Logger.debug("Updating for ${player.name} isConnected=$isConnected isAdded=$isAdded isNear=$isNear isAudioConnected=$isAudioConnected")

        if (isNear && isConnected && isAudioConnected) {
            if (!isAdded)
                addPlayerIfConnected(player)
        } else {
            if (isAdded)
                removePlayer(player)
        }
    }

    private inline fun <reified T : BasePacket> sendPacket(basePacket: T) {
        val json = basePacket.toJson()
        if (DEBUG_CHANGES && players.isNotEmpty())
            Logger.debug("Sending $json")
        players.forEach {
            it.getAudioChannelMeta()?.send(json)
        }
    }

    fun runInBatch(recheckPlayersAfter: Boolean = false, action: SpatialAudio.() -> Unit) {
        isRunningBatch = true
        try {
            action.invoke(this)
            isRunningBatch = false
            sendDeltaUpdate()
            if (recheckPlayersAfter) {
                recheckPlayers()
            }
        } catch (e: Exception) {
        } finally {
            isRunningBatch = false
        }
    }

    fun maxDistance(value: Double) {
        if (packetUpdate.maxDistance.equalsWithPrecision(value)) return
        if (DEBUG_CHANGES)
            Logger.debug("MaxDistance changed ${packetUpdate.maxDistance} vs $value")
        packetUpdate.maxDistance = value

        startDeltaUpdate { maxDistance = value }
        sendDeltaUpdate()
    }

    fun refDistance(value: Double) {
        if (packetUpdate.refDistance.equalsWithPrecision(value)) return
        if (DEBUG_CHANGES)
            Logger.debug("RefDistance changed ${packetUpdate.refDistance} vs $value")
        packetUpdate.refDistance = value

        startDeltaUpdate { refDistance = value }
        sendDeltaUpdate()
    }

    fun volume(value: Double) {
        if (packetUpdate.volume.equalsWithPrecision(value)) return
        if (DEBUG_CHANGES)
            Logger.debug("Volume changed ${packetUpdate.volume} vs $value")
        packetUpdate.volume = value

        startDeltaUpdate { volume = value }
        sendDeltaUpdate()
    }

    fun playing(value: Boolean) {
        if (packetUpdate.playing == value) return
        if (DEBUG_CHANGES)
            Logger.debug("Playing changed")
        packetUpdate.playing = value

        startDeltaUpdate { playing = value }
        sendDeltaUpdate()
    }

    fun loop(value: Boolean) {
        if (packetUpdate.loop == value) return
        if (DEBUG_CHANGES)
            Logger.debug("Loop changed")
        packetUpdate.loop = value

        startDeltaUpdate { loop = value }
        sendDeltaUpdate()
    }

    fun sync(value: Long) {
        if (packetUpdate.sync == value) return
        if (DEBUG_CHANGES)
            Logger.debug("Sync changed")
        packetUpdate.sync = value

        startDeltaUpdate { sync = value }
        sendDeltaUpdate()
    }

    fun rate(value: Double) {
        if (packetUpdate.rate.equalsWithPrecision(value)) return
        if (DEBUG_CHANGES)
            Logger.debug("Rate changed ${packetUpdate.rate} vs $value")
        packetUpdate.rate = value

        startDeltaUpdate { rate = value }
        sendDeltaUpdate()
    }

    fun setLocation(newLocation: Location, recheckPlayers: Boolean = true) {
        setOrientation(newLocation.yaw.toDouble(), newLocation.pitch.toDouble())
        setLocation(newLocation.x, newLocation.y, newLocation.z, recheckPlayers)
    }

    fun setLocation(newLocation: Vector, recheckPlayers: Boolean = true) {
        setLocation(newLocation.x, newLocation.y, newLocation.z, recheckPlayers)
    }

    fun setLocation(x: Double, y: Double, z: Double, recheckPlayers: Boolean = true) {
        if (x.equalsWithPrecision(location.x) && y.equalsWithPrecision(location.y) && z.equalsWithPrecision(location.z)) return
        if (DEBUG_CHANGES)
            Logger.debug("Location changed")

        location.x = x
        location.y = y
        location.z = z

        packetUpdate.x = location.x
        packetUpdate.y = location.y
        packetUpdate.z = location.z
//        packetUpdate.yaw = location.yaw
//        packetUpdate.pitch = location.pitch

        if (recheckPlayers)
            recheckPlayers()

        startDeltaUpdate {
            this.x = location.x
            this.y = location.y
            this.z = location.z
        }
        sendDeltaUpdate()
    }

    fun setOrientation(yaw: Double, pitch: Double) {
        if (location.yaw.toDouble().equalsWithPrecision(yaw) || location.pitch.toDouble()
                .equalsWithPrecision(pitch)
        ) return
        if (DEBUG_CHANGES)
            Logger.debug("Orientation changed")

        location.yaw = yaw.toFloat()
        location.pitch = pitch.toFloat()

        startDeltaUpdate {
            val xz = cos(pitch)
            orientationX = -xz * sin(yaw)
            orientationY = -sin(pitch)
            orientationZ = xz * cos(yaw)
        }
        sendDeltaUpdate()
    }

    fun cleanupPlayers() {
        players.removeAll { !it.isConnected() || it.getAudioChannelMeta() == null }
    }

    companion object {
        private var id = AtomicInteger()
        private var DEBUG_CHANGES = false

        const val SYNC_WITH_GROUP = -1L
        const val SYNC_RESET_EVERY_START = -2L
    }
}