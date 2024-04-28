package net.craftventure.core.map.renderer

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.drawHorizontalLine
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import net.craftventure.database.generated.cvdata.tables.pojos.TeamScore
import net.craftventure.database.generated.cvdata.tables.pojos.TeamScoreMember
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
import kotlin.math.floor


class TeamScoreScoreboardRenderer : CoroutineMapEntryRenderer() {
    private var sortedScores: List<TeamData>? = null
    override val key: String get() = "${mapData?.target}"
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L

    private var mapData: Data? = null
        get() {
            if (field == null) {
                try {
                    field = CvMoshi.adapter(Data::class.java).fromJson(mapEntry!!.data!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return field
        }

    private val backgrounds = DefaultBackgroundHolders()

    override fun invalidate() {
//        logcat { "Invalidate" }
        hasDrawn = false
        sortedScores = null
        mapData = null

        if (renderPass <= 0)
            renderPass = System.currentTimeMillis() + 5000
    }

    override fun shouldRender(): Boolean = sortedScores == null && renderPass <= System.currentTimeMillis() && !hasDrawn

    override suspend fun doRender(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
//        logcat { "Rendering.." }

        try {
            val font = MinecraftFont.Font

            val data = mapData ?: run {
                isLoading = false
                return
            }

            var yOffset = 34 - (128 * data.page)
            val availableHeight = (128 * data.pageCount) - 34 - 10
            val minScoresToDisplay = floor(availableHeight / 48.0).toInt()

            val startDate = data.start
            val endDate = data.end

            val items = MainRepositoryProvider.teamScoreRepository.getTopCounters(
                target = data.target,
                limit = minScoresToDisplay * 3L,
                after = startDate,
                before = endDate,
            )//.let { it + it + it + it + it + it + it }
            val teamData = items.map {
                TeamData(
                    it,
                    MainRepositoryProvider.teamScoreMemberRepository.getMembersForTeam(it.team!!),
                )
            }

            val background = backgrounds.getFor(page = data.page, pageCount = data.pageCount)
                .retrieveRenderedImageBlocking()
            mapCanvas.drawImage(0, 0, background)

            val page = data.page
            val dividerColor = MapPalette.matchColor(Color.decode(data.dividerColor))

            if (page == 0)
                data.title?.let { title ->
                    if (font.isValid(title))
                        mapCanvas.drawText(
                            (64 - (font.getWidth(title) * 0.5)).toInt(), 5, font,
                            "§${MapPalette.matchColor(Color.decode(data.titleColor))};$title"
                        )
                }

            if (page == 0)
                data.subtitle?.let { it.replace("\$period", data.periodDescription() ?: "") }?.let { subtitle ->
                    if (font.isValid(subtitle))
                        mapCanvas.drawText(
                            (64 - (font.getWidth(subtitle) * 0.5)).toInt(), 15, font,
                            "§${MapPalette.matchColor(Color.decode(data.subtitleColor))};$subtitle"
                        )
                }

            val isLastPage = data.pageCount <= 1 || data.page >= data.pageCount - 1

//            mapCanvas.drawText(50, 50, font, "${teamData.size} items")
            teamData.forEachIndexed { index, teamData ->
                if (isLastPage && yOffset + 2 + 10 + (teamData.members.size * 10) + 2 > 128 - 10) return@forEachIndexed
                if (yOffset > 128) return@forEachIndexed
//                logcat { "index=$index yOffset=$yOffset final=${yOffset + 2 + 10 + (teamData.members.size * 10) + 2} for=${(128 * data.pageCount) - 10}" }
                if (index != 0)
                    mapCanvas.drawHorizontalLine(
                        data.dividerStartX,
                        data.dividerEndX,
                        yOffset - 1,
                        dividerColor
                    )

                yOffset += 2
                val scoreDisplay =
                    (data.prefix ?: "") + "${teamData.team.score}" + (data.suffix ?: "")
                if (font.isValid(scoreDisplay))
                    mapCanvas.drawText(
                        (64 - (font.getWidth(scoreDisplay) * 0.5)).toInt(), yOffset, font,
                        "§${MapPalette.matchColor(Color.decode(data.scoreColor))};$scoreDisplay"
                    )

                yOffset += 10
                teamData.members.take(3).forEachIndexed { index, member ->
                    val name = member.member?.toName() ?: "?"
                    if (font.isValid(name))
                        mapCanvas.drawText(
                            (64 - (font.getWidth(name) * 0.5)).toInt(), yOffset, font,
                            "§${MapPalette.matchColor(Color.decode(data.nameColor))};$name"
                        )
                    yOffset += 10
                }
                yOffset += 2
            }

            this.sortedScores = listOf()
            renderPass = System.currentTimeMillis() + 5000
            hasDrawn = true
//            logcat { "Rendered teams" }
//            logcat { "Rendered ${teamData.joinToString { "${it.team.at} / ${it.members.joinToString { "${it.id}" }}" }}" }
        } catch (e: Exception) {
            Logger.capture(e)
//            logcat { "Failed to render" }
        }
    }

    private class TeamData(
        val team: TeamScore,
        val members: List<TeamScoreMember>,
    )

    @JsonClass(generateAdapter = true)
    class Data(
        val target: String,
        val assumeSinglePersonTeams: Boolean = false,
        val page: Int,
        val pageCount: Int = 1,
        val dividerColor: String = defaultDividerColor,
        val titleColor: String = defaultTitleColor,
        val subtitleColor: String = defaultSubtitleColor,
        val dividerStartX: Int = 20,
        val dividerEndX: Int = 128 - 20,
        val scoreColor: String = defaultScoreColor,
        val nameColor: String = defaultNameColor,
        val title: String? = null,
        val subtitle: String? = null,
        val prefix: String? = "",
        val suffix: String? = "",
        val periodType: PeriodType = PeriodType.Ever,
        val periodOffset: Long = 0,
    ) {
        val start: LocalDateTime?
            get() = when (periodType) {
                PeriodType.Ever -> null
                PeriodType.Yearly -> LocalDateTime.now()
                    .with(LocalTime.MIN)
                    .with(TemporalAdjusters.firstDayOfYear())
                    .plusYears(periodOffset)

                PeriodType.Monthly -> LocalDateTime.now()
                    .with(LocalTime.MIN)
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .plusMonths(periodOffset)

                PeriodType.Weekly -> LocalDateTime.now()
                    .with(LocalTime.MIN)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(periodOffset)

                PeriodType.Daily -> LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
                    .plusDays(periodOffset)
            }

        val end: LocalDateTime?
            get() = when (periodType) {
                PeriodType.Ever -> null
                PeriodType.Yearly -> LocalDateTime.now()
                    .with(LocalTime.MAX)
                    .with(TemporalAdjusters.lastDayOfYear())
                    .plusYears(periodOffset)

                PeriodType.Monthly -> LocalDateTime.now()
                    .with(LocalTime.MAX)
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .plusMonths(periodOffset)

                PeriodType.Weekly -> LocalDateTime.now()
                    .with(LocalTime.MAX)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .plusWeeks(periodOffset)

                PeriodType.Daily -> LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                    .plusDays(periodOffset)
            }

        fun periodDescription(): String? = when (periodType) {
            PeriodType.Ever -> "ever"
            PeriodType.Yearly ->
                when (periodOffset) {
                    0L -> "this year"
                    -1L -> "last year"
                    else -> "${start!!.year}"
                }

            PeriodType.Monthly ->
                when (periodOffset) {
                    0L -> "this month"
                    -1L -> "last month"
                    else -> "${start!!.year}/m${start!!.month.value}"
                }

            PeriodType.Weekly ->
                when (periodOffset) {
                    0L -> "this week"
                    -1L -> "last week"
                    else -> "${start!!.year}w${start!!.get(DateUtils.weekFields.weekOfYear())}"
                }

            PeriodType.Daily ->
                when (periodOffset) {
                    0L -> "today"
                    -1L -> "yesterday"
                    else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(start!!)
                }
        }
    }

    enum class PeriodType {
        Ever,
        Yearly,
        Monthly,
        Weekly,
        Daily,
    }
}