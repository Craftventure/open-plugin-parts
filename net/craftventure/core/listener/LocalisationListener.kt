package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.plugin.PluginProvider.isNonProductionServer
import net.craftventure.bukkit.ktx.plugin.PluginProvider.isTestServer
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.util.CVChatColor.serverNotice
import net.craftventure.core.async.executeAsync
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLocaleChangeEvent

class LocalisationListener : Listener {
    @EventHandler
    fun onLocaleChange(event: PlayerLocaleChangeEvent) {
        val player = event.player
        executeAsync {
            MainRepositoryProvider.playerLocaleRepository.createIfNotExists(player.uniqueId, event.locale())
        }

//        logcat { "Title ${Translation.TITLE_JOIN.getTranslation(player)} to component ${Translation.TITLE_JOIN.getTranslation(player)}" }
        TitleManager.display(
            player,
            TitleManager.TitleData.ofTicks(
                id = "localisation",
                title = Translation.TITLE_JOIN.getTranslation(player),
                subtitle = Translation.SUBTITLE_JOIN.getTranslation(player),
                stayTicks = 20 * 6,
                fadeInTicks = 40,
                fadeOutTicks = 40,
            ),
            replace = true,
        )
        player.sendMessage(Translation.MOTD_LOGIN.getTranslation(player)!!)

        val newLocale = event.locale().toString()

        if (newLocale.equals("en_pt", ignoreCase = true)) MainRepositoryProvider.achievementProgressRepository.reward(
            player.uniqueId,
            "talk_like_a_pirate"
        )
        if (newLocale.equals("lol_us", ignoreCase = true)) MainRepositoryProvider.achievementProgressRepository.reward(
            player.uniqueId,
            "talk_like_a_cat"
        )

        if (isNonProductionServer() && !isTestServer() && player.isCrew()) {
            player.sendMessage(
                """$serverNotice 
Welcome to this non-production server. There are multiple commands that are specific to this server that you may want to remember:
/ftp
/shutdown true
/cvreload (reloads several configs)
/audio reload (reloads the audioserver config)
/invalidatecaches (after editing the database)
/script reloadall (after editing a script)
/sync (to sync with the main server)
 
 
"""
            )
        }
    }
}