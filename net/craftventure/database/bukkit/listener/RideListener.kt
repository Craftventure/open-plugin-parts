package net.craftventure.database.bukkit.listener

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.toLocation
import net.craftventure.database.bukkit.util.RideLinkHelper
import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import net.craftventure.database.generated.cvdata.tables.pojos.RideLink
import net.craftventure.database.repository.BaseIdRepository
import net.craftventure.database.type.RideState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.Location

class RideListener : BaseIdRepository.Listener<Ride>() {
    override fun onUpdate(item: Ride) {
        handle(item)
    }

    override fun onMerge(item: Ride) {
        handle(item)
    }

    override fun onRefresh(item: Ride) {
        updateFencesAndCaches(item)
    }

    private fun handle(item: Ride) {
        val rideState = item.state
        item.state = rideState

        if (rideState == RideState.SECRET || rideState == RideState.SECRET_CLOSED) return
//
        if (rideState!!.isOpen) {
            var component = Component.text(
                item.displayName + " is now open" + (if (rideState == RideState.VIP_PREVIEW) " for vips" else "") + "!",
                if (rideState == RideState.VIP_PREVIEW) CVTextColor.BROADCAST_RIDE_VIP_OPEN else CVTextColor.BROADCAST_RIDE_OPEN
            )
            if (item.warpId != null) {
                component += Component.text(" Click here to warp", CVTextColor.serverNoticeAccent)
                component.hoverEvent(Component.text("Click to warp", CVTextColor.CHAT_HOVER).asHoverEvent())
                component.clickEvent(ClickEvent.runCommand("/warp ${item.warpId!!}"))
            }
            Bukkit.getServer().sendMessage(component)
        } else if (rideState == RideState.CLOSED) {
            Bukkit.getServer()
                .sendMessage(Component.text("${item.displayName} is now closed!", CVTextColor.BROADCAST_RIDE_CLOSED))
        } else if (rideState == RideState.MAINTENANCE) {
            Bukkit.getServer().sendMessage(
                Component.text(
                    "${item.displayName} is now in maintenance!",
                    CVTextColor.BROADCAST_RIDE_BROKEN_DOWN
                )
            )
        }

        updateFencesAndCaches(item)
    }

    private fun updateFencesAndCaches(item: Ride) {
        val locationMap = HashMap<RideLink, Location>()
        val rideLinks = MainRepositoryProvider.rideLinkRepository.cachedItems.filter { it.ride == item.id }
        for (rideLink in rideLinks) {
            try {
                locationMap[rideLink] = rideLink.toLocation()
            } catch (e: Exception) {
                Logger.capture(e)
            }
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(PluginProvider.plugin) {
            for ((key) in locationMap) {
                RideLinkHelper.updateLink(item, key)
            }
        }
    }
}