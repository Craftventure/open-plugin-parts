package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.chat.bungee.extension.asPlainText
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.isSign
import net.craftventure.core.inventory.impl.ShopOfferMenu
import net.craftventure.core.ktx.extension.takeIfNotBlank
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.bukkit.listener.ShopCacheListener
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*


class ShopListener : Listener {
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.clickedBlock == null)
            return
        if (PermissionChecker.isCrew(event.player) && (event.player.inventory.itemInMainHand.type.isSign() || event.action == Action.LEFT_CLICK_BLOCK)) {
            return
        }
        if (event.clickedBlock!!.type.isSign()) {
            if (event.action == Action.LEFT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_BLOCK) {
                val sign = event.clickedBlock!!.state as Sign
                val lines = sign.lines.map { it.trim() } as List<String?>

                val type = lines.firstOrNull { it?.startsWith("[") == true }?.removePrefix("[")?.removeSuffix("]")

                when (type?.lowercase(Locale.getDefault())) {
                    "shop" -> {
                        val baseId =
                            sign.lines()
                                .mapNotNull { it.asPlainText().takeIfNotBlank() }
                                .joinToString(" ") { it.replace("[shop]", "") }
                                .replace("-", "_")
                                .lowercase(Locale.getDefault())
                                .replace(" +".toRegex(), " ")
                                .trim()
                        val shopIds = arrayOf(baseId, baseId.replace(" ", ""), baseId.replace(" ", "_"))
                        executeAsync {
                            val cachedShop = shopIds
                                .firstOrNull { ShopCacheListener.cached(it) != null }
                                ?.let { ShopCacheListener.cached(it) }
                            if (cachedShop != null) {
                                ShopOfferMenu(event.player, cachedShop).openAsMenu(event.player)
                            } else {
                                Logger.warn("Shop [${shopIds.joinToString(", ")}] not found", logToCrew = false)
                            }
                        }
                    }
                }
            }
        }
    }
}
