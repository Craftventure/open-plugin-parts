package net.craftventure.core.feature.minigame

import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.isSign
import net.craftventure.core.ktx.util.DateUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

open class LobbySignListener(
    val lobby: Lobby,
    signLocation: Location
) : Listener, Lobby.Listener {
    val signLocation = signLocation.block.location
    private var registered = false
    private var updateTask: Int = 0

    fun register() {
        if (registered) return
        registered = true
        Bukkit.getServer().pluginManager.registerEvents(this, CraftventureCore.getInstance())
        lobby.addListener(this)
        registry.add(this)
        updateTask = executeSync(1, 20, this::update)
        updateSign()
    }

    fun unregister() {
        if (!registered) return
        registered = false
        clearSign()
        HandlerList.unregisterAll(this)
        lobby.removeListener(this)
        registry.remove(this)
        Bukkit.getScheduler().cancelTask(updateTask)
    }

    private fun update() {
        if (lobby.minigame.isRunning)
            updateSign()
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.clickedBlock == null)
            return
        if (event.hand != EquipmentSlot.HAND)
            return
        if (PermissionChecker.isCrew(event.player) && (event.player.inventory.itemInMainHand.type.isSign() || event.action == Action.LEFT_CLICK_BLOCK)) {
            return
        }

        if (event.clickedBlock?.location?.equals(signLocation) == true) {
            if (lobby.isQueued(event.player)) {
                lobby.tryLeave(event.player)
            } else {
                lobby.tryJoin(event.player)
            }
            event.isCancelled = true
            return
        }
    }

    private fun clearSign() {
        (signLocation.block.state as? Sign)?.apply {
            line(0, Component.text(lobby.minigame.displayName))
            line(1, Component.text("Lobby closed").decorate(TextDecoration.BOLD))
            line(2, Component.empty())
            line(3, Component.empty())
            update(true, true)
        }
    }

    private fun updateSign() {
//        Logger.debug("Trying to update sign for ${lobby.minigame.displayName}...")
        (signLocation.block.state as? Sign)?.apply {
            //            Logger.debug("Updating sign ${lobby.minigame.displayName}")
            setLine(0, lobby.minigame.displayName)
            if (lobby.minigame.isRunning) {
                val timeLeft = lobby.minigame.timeLeft ?: 0
                val formattedTime = DateUtils.format(timeLeft, "?")
                setLine(1, "$formattedTime left")
            } else {
                line(1, Component.text("Click to join!").decorate(TextDecoration.BOLD))
            }
            setLine(2, "${lobby.queuedCount}/${lobby.maxPlayers}")
            setLine(3, lobby.state.lobbyDescription)
            update(true, true)
        }
    }

    override fun onUpdated(lobby: Lobby) {
        updateSign()
    }

    companion object {
        private val registry: MutableSet<LobbySignListener> = mutableSetOf()

        fun cleanup(lobby: Lobby) {
            val delete = registry.filter { it.lobby === lobby }
            delete.forEach { it.unregister() }
            registry.removeAll(delete)
        }
    }
}