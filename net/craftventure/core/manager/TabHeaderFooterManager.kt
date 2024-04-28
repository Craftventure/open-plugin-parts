package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.plugin.Environment
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.manager.visibility.VisibilityMeta
import net.craftventure.core.metadata.CvMetadata
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player


object TabHeaderFooterManager {

    private var hasInitialised = false

    //    private var playerCount = 0
    private var onlineCount = 0

    fun init() {
        if (!hasInitialised) {
            hasInitialised = true
            for (player in Bukkit.getOnlinePlayers()) {
                updateMessagesForPlayer(player)
            }
            Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
                //                val time = System.currentTimeMillis()
                onlineCount = Bukkit.getOnlinePlayers().count { it.getMetadata<VisibilityMeta>()?.vanished != true }

                for (player in Bukkit.getOnlinePlayers()) {
                    updateMessagesForPlayer(player)
                }
//                if (System.currentTimeMillis() - time > 5) {
//                    Logger.info("Updating tab took " + (System.currentTimeMillis() - time) + "ms")
//                }
            }, 20, 20)
//            Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
//                executeAsync { playerCount = MainRepositoryProvider.guestStatRepository.count() }
//            }, 20, 20 * 5)
        }
    }

    fun update(player: Player) {
        updateMessagesForPlayer(player)
    }

    private fun updateMessagesForPlayer(player: Player) {
        try {
            val cvMetadata = player.getMetadata<CvMetadata>()
            if (cvMetadata != null) {
                player.sendPlayerListHeader(
                    Component.text("\nWelcome to Craftventure!\n ")
                        .style(Style.style(CVTextColor.serverNoticeAccent, TextDecoration.BOLD))
                )
                val totalOnlineTime = cvMetadata.currentTotalOnlineTimeInMs
                val totalActiveTime = cvMetadata.currentActiveOnlineTimeInMs
                var footerComponent = Component.text("", CVTextColor.guest)
//                footerComponent += "\nJoin us at discord.Craftventure.net"

                val tps = Bukkit.getTPS().firstOrNull() ?: 20.0
                footerComponent += "\nCurrent server performance is "
                footerComponent += when {
                    tps < 12 -> Component.text("just really bad..", NamedTextColor.DARK_RED)
                    tps < 16 -> Component.text("pretty laggy", NamedTextColor.RED)
                    tps < 17.5 -> Component.text("mediocre", NamedTextColor.YELLOW)
                    tps < 19 -> Component.text("not great, not terrible")
                    else -> Component.text("fine")
                }

                footerComponent += "\n\nTotal online time "
                footerComponent += Component.text(
                    DateUtils.format(totalOnlineTime, "unknown"),
                    CVTextColor.serverNotice
                )
                footerComponent += "\nOf which active "
                footerComponent += Component.text(
                    DateUtils.format(totalActiveTime, "unknown"),
                    CVTextColor.serverNotice
                )
                footerComponent += "\n\nThere are "
                footerComponent += Component.text(onlineCount.toString(), CVTextColor.serverNotice)
                footerComponent += " players currently online"
//                footerComponent += "\n\nA total of $playerCount unique guests have joined us"

                if (player.isCrew()) {
                    val averageTickTime = Bukkit.getAverageTickTime()
                    footerComponent += Component.text("\n======== ", NamedTextColor.DARK_GRAY)
                    CraftventureCore.getInstance().environment.apply {
                        footerComponent += Component.text(
                            this.name.lowercase(),
                            if (this == Environment.PRODUCTION) NamedTextColor.DARK_GRAY else NamedTextColor.RED
                        )
                    }
                    footerComponent += Component.text(" ========", NamedTextColor.DARK_GRAY)
                    footerComponent += "\nTPS=${Bukkit.getServer().tps.firstOrNull()?.format(2)} " +
                            "avgTick=${averageTickTime.format(2)}ms / " +
                            "${((averageTickTime / 50.0) * 100).format(2)}%"
                }

                player.sendPlayerListFooter(footerComponent)
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }

    }
}
