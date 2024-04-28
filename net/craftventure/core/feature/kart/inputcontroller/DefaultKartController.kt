package net.craftventure.core.feature.kart.inputcontroller

import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.serverevent.PacketPlayerSteerEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener


class DefaultKartController : KartController, Listener {
    private var kart: Kart? = null
    private var sideways = 0.0f
    private var forward = 0.0f
    private var jumping = false
    private var dismounting = false

    private var controllerForwards: Float = 0.0f
    private var controllerBackwards: Float = 0.0f

    override fun sideways(): Float = sideways
    override fun forward(): Float = forward

    override fun isHandbraking(): Boolean = jumping
    override fun isDismounting(): Boolean = dismounting

    override fun resetValues() {
        sideways = 0f
        forward = 0f
        jumping = false
        dismounting = false
        controllerForwards = 0f
        controllerBackwards = 0f
    }

    override fun start(kart: Kart) {
        this.kart = kart
        Bukkit.getPluginManager().registerEvents(this, CraftventureCore.getInstance())
    }

    override fun stop() {
        this.kart = null
        HandlerList.unregisterAll(this)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerSteerEvent(event: PacketPlayerSteerEvent) {
        if (event.player == kart?.player) {
//            Logger.debug("Handling steer ${event.isDismounting}")
//            Logger.debug("Steering player=${event.player.name} sideways=${event.sideways.format(2)} forwards=${event.forwards.format(2)} dismounting=${event.isDismounting}")
            sideways = event.sideways
            sideways = when {
                sideways < -0.1 -> -1f
                sideways > 0.1 -> 1f
                else -> 0f
            }
            forward = event.forwards
            forward = when {
                forward < -0.1 -> -1f
                forward > 0.1 -> 1f
                else -> 0f
            }
            jumping = event.isJumping
            dismounting = event.isDismounting
            event.isCancelled = true
        }
    }
}
