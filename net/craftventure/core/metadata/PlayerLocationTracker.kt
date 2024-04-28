package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.event.AsyncPlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.bukkit.ktx.extension.asString
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerLocationTracker(
    val player: Player
) : BasePlayerMetadata(player) {
    private var lastLocation: Location = player.location
        set(value) {
            field = value
            dirty = true
        }
    protected var lastLocationChanged = false
    protected var lastLookChanged = false
    protected var lastTeleport = false
    var dirty = false
        private set

    private val backLocations = mutableListOf<Location>()

    private var leaveLocation: Location? = null
    private var leaveTick = 0

    fun getLeaveLocation() = leaveLocation?.takeIf {
//        Logger.debug("${Bukkit.getCurrentTick()} vs $leaveTick")
        Bukkit.getCurrentTick() == leaveTick
    }

    fun setLeaveLocation(location: Location) {
//        Logger.debug("Setting leave location")
        leaveLocation = location
        leaveTick = Bukkit.getCurrentTick()
    }

    fun pushBackLocation(location: Location) {
        backLocations.add(location)
        while (backLocations.size > 5) {
            backLocations.removeFirst()
        }
    }

    fun popBackLocation(): Location? {
        return backLocations.removeLastOrNull()
    }

    override fun debugComponent() = Component.text(
        "dirt=$dirty lastTeleport=$lastTeleport lastLookChanged=$lastLookChanged lastLocationChanged=$lastLocationChanged lastLocation=${
            lastLocation.toVector().asString()
        }"
    )

    fun markClean() {
        dirty = false
    }

    fun handleDirtyAsync() {
        if (dirty) {
            val event = AsyncPlayerLocationChangedEvent(
                player = player,
                to = lastLocation,
                locationChanged = lastLocationChanged,
                lookChanged = lastLookChanged,
                isTeleport = lastTeleport
            )
            Bukkit.getServer().pluginManager.callEvent(event)
            markClean()
        }
    }

    /**
     * @return true if allowed, false otherwise
     */
    fun updateLocation(location: Location = player.location, isTeleport: Boolean): Boolean {
        val locationChanged = lastLocation.x != location.x ||
                lastLocation.y != location.y ||
                lastLocation.z != location.z
        val lookChanged = lastLocation.yaw != location.yaw ||
                lastLocation.pitch != location.pitch

        if (locationChanged || lookChanged) {
            val clonedLocation = location.clone()
//            Logger.debug("Location of ${player.name} changed $locationChanged/$lookChanged")
            val event = PlayerLocationChangedEvent(
                player = player,
                from = lastLocation,
                to = clonedLocation,
                locationChanged = locationChanged,
                lookChanged = lookChanged,
                isTeleport = isTeleport
            )
            Bukkit.getServer().pluginManager.callEvent(event)
            if (!event.isCancelled) {
                lastLocation = clonedLocation
                lastLocationChanged = locationChanged
                lastTeleport = isTeleport
                lastLookChanged = lookChanged
            }
            return !event.isCancelled
        }
        return true
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { PlayerLocationTracker(player) }
    }

    companion object {
        @JvmOverloads
        @JvmStatic
        fun setLeaveLocation(player: Player, location: Location, applyTeleport: Boolean = false) {
            player.setLeaveLocation(location, applyTeleport)
        }
    }
}


fun Player.setLeaveLocation(location: Location, applyTeleport: Boolean = false) {
    if (applyTeleport) {
        teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    getMetadata<PlayerLocationTracker>()?.setLeaveLocation(location)
}