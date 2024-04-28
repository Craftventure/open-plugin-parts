package net.craftventure.core.map.renderer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.drawHorizontalLine
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.repository.MinigameScoreRepository
import net.craftventure.database.type.MinigameScoreType
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapPalette
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import java.awt.Color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters


class MinigameScoreboardRenderer : MapEntryRenderer() {
    private var sortedScores: List<MinigameScore>? = null
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L

    override val key: String get() = "${mapData?.name}"

    private var mapData: MinigameScoreMapData? = null
        get() {
            if (field == null) {
                try {
                    field = CvMoshi.adapter(MinigameScoreMapData::class.java).fromJson(mapEntry!!.data!!)
//                    Logger.console("Converting ${mapEntry.data} to ${value?.name} ${value?.backgroundName}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return field
        }
    private val backgroundHolders = DefaultBackgroundHolders()

    override fun invalidate() {
        hasDrawn = false
        sortedScores = null
        mapData = null
    }

    fun invalidateForGame(gameId: String) {
//        Logger.info("Invalidating minigamescore $gameId for data ${mapData?.name}? ${mapData?.name == gameId}")
        if (mapData?.name == gameId) {
            invalidate()
            if (renderPass <= 0)
                renderPass = System.currentTimeMillis() + 5000
        }
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        if (sortedScores == null && renderPass <= System.currentTimeMillis() && !hasDrawn) {
            if (!isLoading) {
                isLoading = true
                executeAsync {
                    try {
//                        Logger.console("Reloading ridescores ${mapData?.name}")
                        val background =
                            backgroundHolders.getFor(page = mapData!!.page, pageCount = mapData!!.pageCount)
                                .retrieveRenderedImageBlocking()
                        val mapData = mapData
                        if (background == null || mapData == null) {
                            isLoading = false
                            return@executeAsync
                        }
//                        Logger.console("Drawing ridescores ${mapData.name}")

                        mapCanvas.drawImage(0, 0, background)

                        val startDate = mapData.start
                        val endDate = mapData.end

//                        if (mapData.periodType != PeriodType.EVER)
//                            Logger.info("Date for ${mapData.periodType} = $start/$end ${startDate!!.time}/${endDate!!.time}")

                        val font = MinecraftFont.Font
                        val items = MainRepositoryProvider.minigameScoreRepository
                            .getTopCounters(
                                gameId = mapData.name,
                                limit = mapData.limit,
                                type = mapData.type,
                                scoreAscending = mapData.sortScoreAscending,
                                includeCrew = mapData.includeCrew,
                                after = startDate,
                                before = endDate,
                                scoreAggregate = mapData.scoreAggregate
                            )
//                        if (mapData.periodType != PeriodType.EVER)
//                            Logger.info("Results ${items.size} items")

                        val page = mapData.page
                        val dividerColor = MapPalette.matchColor(Color.decode(mapData.dividerColor))

                        if (page == 0)
                            mapData.title?.let { title ->
                                if (font.isValid(title))
                                    mapCanvas.drawText(
                                        (64 - (font.getWidth(title) * 0.5)).toInt(), 5, font,
                                        "§${MapPalette.matchColor(Color.decode(mapData.titleColor))};$title"
                                    )
                            }

                        if (page == 0)
                            mapData.subtitle?.let { it.replace("\$period", mapData.periodDescription() ?: "") }
                                ?.let { subtitle ->
                                    if (font.isValid(subtitle))
                                        mapCanvas.drawText(
                                            (64 - (font.getWidth(subtitle) * 0.5)).toInt(), 15, font,
                                            "§${MapPalette.matchColor(Color.decode(mapData.subtitleColor))};$subtitle"
                                        )
                                }

                        val yOffset = -128 * mapData.page
                        for ((i, minigameScore) in items.withIndex()) {
                            val count = "#${i + 1} " + (mapData.prefix ?: "") + (when (mapData.scoreType) {
                                ScoreType.RAW -> minigameScore.score?.toString() ?: "?"
                                ScoreType.MS -> DateUtils.formatWithMillis(
                                    minigameScore.score!!.toLong(), "?"
                                )
                                ScoreType.RACE_MS -> {
                                    val timeInMillis = minigameScore.score!!.toLong()
                                    val minutes = timeInMillis / 60_000
                                    val seconds = (timeInMillis % 60_000) / 1000
                                    val millis = timeInMillis % 1000
                                    "${minutes}:${seconds}.${"%03d".format(millis)}"
                                }
                            }) + (mapData.suffix ?: "")
                            val name = minigameScore.uuid?.toName()

                            if (i != 0)
                                mapCanvas.drawHorizontalLine(
                                    mapData.dividerStartX,
                                    mapData.dividerEndX,
                                    yOffset + 34 - 2 + (i * 22),
                                    dividerColor
                                )

                            if (font.isValid(count))
                                mapCanvas.drawText(
                                    (64 - (font.getWidth(count) * 0.5)).toInt(), yOffset + 34 + (i * 22), font,
                                    "§${MapPalette.matchColor(Color.decode(mapData.scoreColor))};$count"
                                )
                            if (name != null && font.isValid(name))
                                mapCanvas.drawText(
                                    (64 - (font.getWidth(name) * 0.5)).toInt(), yOffset + 34 + (i * 22) + 10, font,
                                    "§${MapPalette.matchColor(Color.decode(mapData.nameColor))};$name"
                                )
//                            else
//                                Logger.console("Invalid map text '$count'")
                        }
                        this.sortedScores = items
                        renderPass = System.currentTimeMillis() + 5000
                        hasDrawn = true
                    } catch (e: Exception) {
                        Logger.capture(e)
                    }

                    isLoading = false
                }
            }
        }
//        if (!hasDrawn) {
//            val color = mapCanvas.getPixel(0, 0)
//            mapCanvas.setPixel(0, 0, color)
//        }
//        if (bufferedImage != null && !hasDrawn) {
//            val start = System.currentTimeMillis()
//            mapCanvas.drawImage(0, 0, bufferedImage)
//            Logger.console("Drawing took %s for %s (%s)", (System.currentTimeMillis() - start), mapEntry.mapId, mapEntry.name)
//            hasDrawn = true
//        }
    }

    @JsonClass(generateAdapter = true)
    open class MinigameScoreMapData : RideScoreboardRenderer.RideScoreMapdata() {
        var limit = 4L

        @Json(name = "sort_score_ascending")
        var sortScoreAscending = false

        var type: MinigameScoreType? = null

        @Json(name = "include_crew")
        var includeCrew: Boolean = false

        @Json(name = "period_type")
        var periodType: PeriodType = PeriodType.EVER

        @Json(name = "period_offset")
        var periodOffset: Long = 0

        @Json(name = "score_type")
        var scoreType: ScoreType = ScoreType.MS

        @Json(name = "score_aggregate")
        var scoreAggregate: MinigameScoreRepository.ScoreAggregate = MinigameScoreRepository.ScoreAggregate.MIN

        val start: LocalDateTime?
            get() = when (periodType) {
                PeriodType.EVER -> null
                PeriodType.YEARLY -> LocalDateTime.now()
                    .with(LocalTime.MIN)
                    .with(TemporalAdjusters.firstDayOfYear())
                    .plusYears(periodOffset)
                PeriodType.MONTHLY -> LocalDateTime.now()
                    .with(LocalTime.MIN)
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .plusMonths(periodOffset)
                PeriodType.WEEKLY -> LocalDateTime.now()
                    .with(LocalTime.MIN)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(periodOffset)
                PeriodType.DAILY -> LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
                    .plusDays(periodOffset)
            }

        val end: LocalDateTime?
            get() = when (periodType) {
                PeriodType.EVER -> null
                PeriodType.YEARLY -> LocalDateTime.now()
                    .with(LocalTime.MAX)
                    .with(TemporalAdjusters.lastDayOfYear())
                    .plusYears(periodOffset)
                PeriodType.MONTHLY -> LocalDateTime.now()
                    .with(LocalTime.MAX)
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .plusMonths(periodOffset)
                PeriodType.WEEKLY -> LocalDateTime.now()
                    .with(LocalTime.MAX)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .plusWeeks(periodOffset)
                PeriodType.DAILY -> LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                    .plusDays(periodOffset)
            }

        fun periodDescription(): String? = when (periodType) {
            PeriodType.EVER -> "ever"
            PeriodType.YEARLY ->
                when (periodOffset) {
                    0L -> "this year"
                    -1L -> "last year"
                    else -> "${start!!.year}"
                }
            PeriodType.MONTHLY ->
                when (periodOffset) {
                    0L -> "this month"
                    -1L -> "last month"
                    else -> "${start!!.year}/m${start!!.month.value}"
                }
            PeriodType.WEEKLY ->
                when (periodOffset) {
                    0L -> "this week"
                    -1L -> "last week"
                    else -> "${start!!.year}w${start!!.get(DateUtils.weekFields.weekOfYear())}"
                }
            PeriodType.DAILY ->
                when (periodOffset) {
                    0L -> "today"
                    -1L -> "yesterday"
                    else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(start!!)
                }
        }
    }

    enum class ScoreType {
        RAW,
        MS,
        RACE_MS,
    }

    enum class PeriodType {
        EVER,
        YEARLY,
        MONTHLY,
        WEEKLY,
        DAILY
    }
}