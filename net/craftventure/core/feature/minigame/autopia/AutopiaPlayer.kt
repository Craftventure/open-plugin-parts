package net.craftventure.core.feature.minigame.autopia

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.ktx.extension.asOrdinalAppended
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.type.MinigameScoreType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*

class AutopiaPlayer(
    val player: Player,
    kart: Kart,
    val color: Autopia.KartColor
) {
    var kart = kart

    init {
        this.kart = kart
    }

    var trackPointIndex = 0
    var lap = 1
    var finishTime = -1L
        private set
    var lapStartingTime = System.currentTimeMillis()

    var distanceFromTrackPoint = 0.0
        private set
    var nearestDistanceFromTrackPoint = 0.0
        private set

    fun recalculateDistanceFromTrackPoint(autopiaLevel: AutopiaLevel) {
        val index = if (trackPointIndex + 1 < autopiaLevel.track.points.size)
            trackPointIndex + 1
        else
            0
        val location = autopiaLevel.track.points[index].location
        distanceFromTrackPoint = location.distanceSquared(player.location)
        nearestDistanceFromTrackPoint = Math.min(nearestDistanceFromTrackPoint, distanceFromTrackPoint)
    }

    fun squaredDistanceFromTrackPoint() = distanceFromTrackPoint

    fun hasFinished(autopiaLevel: AutopiaLevel) = lap > autopiaLevel.laps

    fun moveToNextTrackPoint(autopia: Autopia, autopiaLevel: AutopiaLevel, position: Int): Boolean {
        if (!player.isInsideVehicle) {
            Logger.warn("Skipping player update for ${player.name}: is not karting", logToCrew = false)
            return false
        }
//        Logger.info("laps=${autopiaLevel.laps} points=${autopiaLevel.track.points.size} point=$trackPointIndex")
        if (autopiaLevel.track.points.size <= trackPointIndex + 1 || (autopiaLevel.laps == 1 && autopiaLevel.track.points.size - 1 <= trackPointIndex + 1)) {
            trackPointIndex = 0
            lap++

            val lapScore = MinigameScore(
                UUID.randomUUID(),
                player.uniqueId,
                autopia.internalName,
                (System.currentTimeMillis() - lapStartingTime).toInt(),
                LocalDateTime.now(),
                MinigameScoreType.ROUND,
                null,
                player.isCrew()
            )

//            Logger.info("Autopia lap score is ${lapScore.score} for ${player.name} (formatted: ${DateUtils.formatWithMillis(lapScore.score
//                    ?: -1, "?")})")

            if (autopia.saveScores)
                executeAsync {
                    if (!MainRepositoryProvider.minigameScoreRepository.createSilent(lapScore))
                        Logger.severe(
                            "Failed to create minigame score for ${player.name} > $lapScore",
                            logToCrew = false
                        )
                }

            lapStartingTime = System.currentTimeMillis()

            if (finishTime <= 0 && hasFinished(autopiaLevel)) {
                finishTime = System.currentTimeMillis()
                val finishScore = MinigameScore(
                    UUID.randomUUID(),
                    player.uniqueId,
                    autopia.internalName,
                    (finishTime - autopia.playStartTime).toInt(),
                    LocalDateTime.now(),
                    MinigameScoreType.TOTAL,
                    null,
                    player.isCrew()
                )
                if (autopia.saveScores)
                    executeAsync {
                        if (!MainRepositoryProvider.minigameScoreRepository.createSilent(finishScore))
                            Logger.severe(
                                "Failed to create minigame score for ${player.name} > $finishScore",
                                logToCrew = false
                            )
                    }

                autopia.players.forEach {
                    it.player.sendMessage(CVTextColor.serverNotice + "${player.name} has finished")
                }

                player.sendTitleWithTicks(
                    10,
                    40,
                    10,
                    NamedTextColor.GOLD,
                    "Finished ${position.asOrdinalAppended()}",
                    NamedTextColor.YELLOW,
                    "Laptime " + DateUtils.formatWithMillis(lapScore.score!!.toLong(), "?")
                )
            } else {
                player.sendTitleWithTicks(
                    10,
                    40,
                    10,
                    NamedTextColor.GOLD,
                    "Lap $lap/${autopiaLevel.laps}",
                    NamedTextColor.YELLOW,
                    "Laptime " + DateUtils.formatWithMillis(lapScore.score!!.toLong(), "?")
                )
            }
        } else
            trackPointIndex++

        nearestDistanceFromTrackPoint =
            player.location.distanceSquared(autopiaLevel.track.points[trackPointIndex].location)
        return true
    }

    fun currentTrackPoint(autopiaLevel: AutopiaLevel): AutopiaTrackPoint {
        return if (autopiaLevel.track.points.size <= trackPointIndex)
            autopiaLevel.track.points[0]
        else
            autopiaLevel.track.points[trackPointIndex]
    }

    fun nextTrackPoint(autopiaLevel: AutopiaLevel): AutopiaTrackPoint {
        return if (autopiaLevel.track.points.size <= trackPointIndex + 1)
            autopiaLevel.track.points[0]
        else
            autopiaLevel.track.points[trackPointIndex + 1]
    }
}
