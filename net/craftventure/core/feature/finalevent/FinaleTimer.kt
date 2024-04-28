package net.craftventure.core.feature.finalevent

import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.bukkit.ktx.plugin.Environment
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.extension.utcMillis
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.utils.GameTimeUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.RideState
import org.bukkit.Bukkit
import org.bukkit.GameRule
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object FinaleTimer {
    val endingDate = ZonedDateTime.parse("2024-04-21T22:00:00+02:00").toLocalDateTime()
    private val endingEndDate = ZonedDateTime.parse("2024-04-28T22:00:00+02:00").toLocalDateTime()
    private var scheduledNextPlay: ScheduledFuture<*>? = null
    private var scheduledPrepare: ScheduledFuture<*>? = null

    private var hasStartedFinale = false

    var nextPlay: LocalDateTime? = null
        private set

    init {
        scheduleOrExecuteCinematicPrepare()
        scheduleNextPlay()
    }

    fun onCinematicCancelled() {
        scheduleNextPlay()
    }

    private fun scheduleOrExecuteCinematicPrepare() {
        val prepareAt = endingDate.minusMinutes(10)
        val now = LocalDateTime.now()
        if (now > endingEndDate) return

        if (prepareAt < now) {
            startFinaleMode()
        } else {
            scheduledPrepare?.cancel(false)
            scheduledPrepare = CraftventureCore.getScheduledExecutorService().schedule({
                startFinaleMode()
            }, prepareAt.utcMillis - now.utcMillis, TimeUnit.MILLISECONDS)
        }
    }

    private fun calculateNextPlayTime(): LocalDateTime {
        val now = LocalDateTime.now()
        if (now > endingEndDate) return LocalDateTime.MAX

        var pickedDate: LocalDateTime = endingDate
        while (pickedDate < now) {
            pickedDate = pickedDate.plusMinutes(20)
        }

        return pickedDate
    }

    private fun scheduleNextPlay() {
        val nextPlay = calculateNextPlayTime()
        this.nextPlay = nextPlay
        val now = LocalDateTime.now()
        if (now > endingEndDate) return

        val offset = nextPlay.utcMillis - now.utcMillis
        if (offset <= 0) {
            runFinale()
        } else {
            logcat { "Scheduling next cinematic in ${offset.toDuration(DurationUnit.MILLISECONDS)} ($offset)" }

            scheduledNextPlay?.cancel(false)
            scheduledNextPlay = CraftventureCore.getScheduledExecutorService().schedule({
                runFinale()
            }, offset, TimeUnit.MILLISECONDS)
        }
    }

    private fun runFinale() {
        executeSync { FinaleCinematic.prepareStart() }
        executeSync(20 * 4) {
            FinaleCinematic.start()
        }
    }

    fun setup() {}

    fun startFinaleMode() {
        logcat { "Starting CV2 finale mode" }
        Bukkit.getServer().worlds.first().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        Bukkit.getServer().worlds.first().time = GameTimeUtils.hoursMinutesToTicks(12, 0)

        if (!hasStartedFinale)
            executeAsync {
                if (CraftventureCore.getInstance().environment == Environment.PRODUCTION)
                    MainRepositoryProvider.rideRepository.cachedItems.forEach {
                        if (it.state != RideState.CLOSED)
                            MainRepositoryProvider.rideRepository.setState(it, RideState.CLOSED)
                    }

                val message = CVTextColor.serverNoticeAccent + "The last ride dispatches have just commenced"
                Bukkit.getOnlinePlayers().forEach {
                    it.sendMessage(message)
                }
                hasStartedFinale = true
            }

        FeatureManager.disableFeature(FeatureManager.Feature.KART_SPAWN_AS_USER)
        FeatureManager.disableFeature(FeatureManager.Feature.SPATIAL_SOUNDS)
        FeatureManager.disableFeature(FeatureManager.Feature.BALLOON_ACTIVATE)
        FeatureManager.disableFeature(FeatureManager.Feature.CLOTHING_PARTICLES)
        FeatureManager.disableFeature(FeatureManager.Feature.SKATES_ENABLED)
        FeatureManager.disableFeature(FeatureManager.Feature.VIEW_OTHER_PLAYERS)
        FeatureManager.disableFeature(FeatureManager.Feature.MINIGAME_JOIN)
//        FeatureManager.disableFeature(FeatureManager.Feature.AUDIOSERVER_TRACKING)
//        FeatureManager.disableFeature(FeatureManager.Feature.AUDIOSERVER_UPDATING)
//        FeatureManager.disableFeature(FeatureManager.Feature.SCENE_ACTION_SCHEMATIC_PASTING)
        FeatureManager.disableFeature(FeatureManager.Feature.AUTOMATED_SCHEMATIC_PASTING)
        FeatureManager.disableFeature(FeatureManager.Feature.SHOPS_PRESENTER)
        FeatureManager.disableFeature(FeatureManager.Feature.JUMP_PUZZLE_JOIN)
        FeatureManager.disableFeature(FeatureManager.Feature.DRESSING_ROOM)
    }
}