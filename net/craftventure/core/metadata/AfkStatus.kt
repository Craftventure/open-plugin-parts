package net.craftventure.core.metadata

import net.craftventure.audioserver.AudioServer
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.core.util.SpaceHelper
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.isOperatingSomewhere
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.serverevent.AfkStatusChangeEvent
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.TimeUnit

class AfkStatus(
    private val player: Player
) : BasePlayerMetadata(player) {
    private var lastWalkActivity = System.currentTimeMillis()
    private var lastActivity = System.currentTimeMillis()
    private var lastOperatorActivity = System.currentTimeMillis()
    private var location: Location? = null
    private var bukkitRunnable: BukkitRunnable? = null
    private var title: Title = generateTitle(player)

    private var totalSessionAfkTime: Long = 0
    private var currentAfkStartTime: Long = 0

    private val titleId = "afkTitle"

    var isAfk = false
        private set(afk) {
            if (afk != this.isAfk) {
                val event = AfkStatusChangeEvent(player, afk)
                Bukkit.getPluginManager().callEvent(event)
                if (event.isCancelled) return

                if (DEBUG)
                    Logger.debug("%s AFK %s", logToCrew = false, params = *arrayOf(player.name, afk))
                if (afk) {
                    currentAfkStartTime = System.currentTimeMillis()
                    executeAsync {
                        MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "afk")
                    }
                } else {
                    totalSessionAfkTime += System.currentTimeMillis() - currentAfkStartTime
                    currentAfkStartTime = 0
                }

//                AudioServerApi.sendKeyValue(player, "afk", afk)
                field = afk

                if (this.isAfk) {
                    title = generateTitle(player)
                    sendAfkTitle()
                } else {
                    TitleManager.remove(player, TitleManager.Type.Afk)
                }
            }
        }

    init {
        bukkitRunnable = object : BukkitRunnable() {
            override fun run() = updateAfkStatus()
        }
        bukkitRunnable!!.runTaskTimer(CraftventureCore.getInstance(), (20 * 3).toLong(), (20 * 3).toLong())
    }

    private fun updateAfkStatus() {
        val afk = isActivityAfk
        if (afk && !this.isAfk) {
            isAfk = true
        } else if (!afk && this.isAfk) {
            isAfk = false
        }
//        if (this.isAfk && currentAfkStartTime < System.currentTimeMillis() - (60 * 60 * 1000)) {
//            player.kickPlayer(CVChatColor.COMMAND_GENERAL + "You were AFK for a looooonnggg time... Feel free to rejoin though!")
//        }
    }

    fun stop() {
        bukkitRunnable?.cancel()
        bukkitRunnable = null
    }

    private fun sendAfkTitle() {
        if (player.gameMode != GameMode.SPECTATOR)
            player.displayTitle(
                TitleManager.TitleData.ofTicks(
                    id = titleId,
                    type = TitleManager.Type.Afk,
                    title = title.titleComponent,
                    subtitle = title.subtitleComponent,
                    100,
                    10000000,
                    30,
                )
            )
    }

    fun locationUpdated(newLocation: Location) {
        if (location == null) {
            location = newLocation.clone()
            updateLastWalkActivity()
        } else {
            val distanceSquared = location!!.distanceSquared(newLocation)
            if (distanceSquared > 12 || distanceSquared > 6 && !isAfk) {
                location!!.x = newLocation.x
                location!!.y = newLocation.y
                location!!.z = newLocation.z
                updateLastWalkActivity()
            }
        }
    }

    fun updateLastActivity() {
        lastActivity = System.currentTimeMillis()
        updateAfkStatus()
    }

    fun updateLastWalkActivity() {
        lastActivity = System.currentTimeMillis()
        lastWalkActivity = lastActivity
        updateAfkStatus()
    }

    fun updateLastOperatorActivity() {
        lastOperatorActivity = System.currentTimeMillis()
        updateAfkStatus()
    }

    private val isActivityAfk: Boolean
        get() {
            if (!player.isConnected()) {
                return false
            }
            val vehicleMeta = player.vehicle?.getMetadata<TypedInstanceOwnerMetadata>()
            if (vehicleMeta != null) {
//                Logger.debug("Updating vehicle status for type $instanceType")
                val kart = vehicleMeta.kart
                if (kart != null) {
                    lastActivity = Math.max(lastActivity, kart.lastInput)
                    lastWalkActivity = lastActivity
                } else {
                    lastActivity = System.currentTimeMillis()
                    lastWalkActivity = lastActivity
                }
            }
            val isOperating = player.isOperatingSomewhere()

            val settings = CraftventureCore.getSettings()
            return (lastActivity < System.currentTimeMillis() - settings.getAfkTimeout(TimeUnit.MILLISECONDS) ||
                    lastWalkActivity < System.currentTimeMillis() - settings.getAfkWalkTimeout(TimeUnit.MILLISECONDS)) &&
                    (!isOperating || lastOperatorActivity < System.currentTimeMillis() - settings.getAfkOperatorTimeout(
                        TimeUnit.MILLISECONDS
                    ))
        }

    fun setAfk() {
        lastActivity = System.currentTimeMillis() - 3 * 1000 * 60
        lastWalkActivity = System.currentTimeMillis() - 7 * 1000 * 60
        lastOperatorActivity = System.currentTimeMillis() - 2 * 1000 * 60
    }

    val totalAfkMillisThisSession: Long
        get() = totalSessionAfkTime + if (currentAfkStartTime > 0) System.currentTimeMillis() - currentAfkStartTime else 0

    data class Title(
        val color: TextColor = NamedTextColor.GOLD,
        val title: String? = null,
        val subtitleColor: TextColor = NamedTextColor.GOLD,
        val subtitle: String? = null,
        val active: () -> Boolean = { true },
    ) {
        val titleComponent =
            Component.text(title ?: "", subtitleColor)
        val subtitleComponent = Component.text()
            .append(Component.text(SpaceHelper.width(-600)))
            .append(Component.text("\uE0ED", NamedTextColor.BLACK))
            .append(Component.text(SpaceHelper.width(-600)))
            .append(Component.text(subtitle ?: "", subtitleColor))
            .build()
    }

    companion object {
        private val DEBUG = false

        fun generateTitle(player: Player): Title = listOf(
            Title(
                NamedTextColor.DARK_AQUA, "A LONG TIME AGO",
                NamedTextColor.DARK_AQUA, "IN A THEMEPARK FAR FAR AWAY..."
            ),
            Title(
                NamedTextColor.GRAY, "\"Brb\", ${player.name} said",
                NamedTextColor.DARK_GRAY, "Never to be seen again..."
            ),
            Title(
                NamedTextColor.GREEN, "Keep calm",
                NamedTextColor.GREEN, "And stay AFK"
            ),
            Title(
                NamedTextColor.DARK_RED, "Eating cookies?",
                NamedTextColor.RED, "Yeah, looks like you're eating cookies. Eat well!"
            ),
            Title(
                CVTextColor.serverNoticeAccent, "VentureCoins...",
                CVTextColor.serverNotice, "You're not getting them right now"
            ),
            Title(
                CVTextColor.serverNoticeAccent, "Are you back yet?",
                CVTextColor.serverNotice, "We miss you :("
            ),
            Title(
                NamedTextColor.DARK_PURPLE, "AFK much " + player.name + "?",
                NamedTextColor.DARK_PURPLE, "Enjoy your view!"
            ),
            Title(
                NamedTextColor.DARK_GREEN, "Ahhhh, being afk...",
                NamedTextColor.DARK_GREEN, "The best time to follow @Craftventure on Twitter!"
            ),
            Title(
                CVTextColor.serverNoticeAccent, "Tried connecting to the AudioServer?",
                CVTextColor.serverNotice, "Looks like you have the time to connect now! /audio",
                active = { AudioServer.server?.hasJoined(player) == false }
            ),
            Title(
                NamedTextColor.DARK_GREEN, "Subscribe to us on YouTube",
                NamedTextColor.DARK_GREEN, "youtube.Craftventure.net"
            ),
            Title(
                NamedTextColor.BLUE, "Discord user?",
                NamedTextColor.BLUE, "Join discord.Craftventure.net!"
            ),
            Title(
                NamedTextColor.BLUE, "Playing the winter event...",
                NamedTextColor.BLUE, "Mans not hot",
                active = { DateUtils.isWinter }
            ),
            Title(
                NamedTextColor.BLUE, "You're AFK..",
                NamedTextColor.BLUE, "On Roller Coaster Day???",
                active = { DateUtils.isCoasterDay }
            ),
            Title(
                NamedTextColor.BLUE, "Going AFK during Halloween...",
                NamedTextColor.BLUE, "You've got some nerves...",
                active = { DateUtils.isHalloween }
            ),
            Title(
                NamedTextColor.BLUE, "Some people looking for eggs...",
                NamedTextColor.BLUE, "Some trying to increase their AFK time...",
                active = { DateUtils.isEastern }
            ),
            Title(
                NamedTextColor.BLUE, "Going AFK.. now?...",
                NamedTextColor.BLUE, "I have a bad feeling about this...",
                active = { DateUtils.isStarWarsDay }
            ),
            Title(
                NamedTextColor.BLUE, "Want better AFK performance?",
                NamedTextColor.BLUE, "Install the sodium and iris mods!",
            ),
        ).filter { it.active() }.random()
    }

    override fun debugComponent(): Component? = Component.text("Not set yet")
}
