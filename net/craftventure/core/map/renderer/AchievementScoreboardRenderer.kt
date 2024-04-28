package net.craftventure.core.map.renderer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.drawHorizontalLine
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.AchievementProgress
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapPalette
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import java.awt.Color
import kotlin.math.floor


class AchievementScoreboardRenderer : MapEntryRenderer() {
    private var items: List<AchievementProgress>? = null
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L

    override val key: String get() = "${mapData?.name}"
    private val backgroundHolders = DefaultBackgroundHolders()

    private var mapData: Mapdata? = null
        get() {
            if (field == null) {
                try {
                    field = CvMoshi.adapter(Mapdata::class.java).fromJson(mapEntry!!.data!!)
//                    Logger.console("Converting ${mapEntry.data} to ${value?.name} ${value?.backgroundName}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return field
        }

    override fun invalidate() {
        hasDrawn = false
        items = null
        mapData = null
    }

    fun invalidateForAchievement(achievementId: String) {
        if (mapData?.achievementId == achievementId) {
//            Logger.info("Invalidating achievement scoreboard")
            invalidate()
            if (renderPass <= System.currentTimeMillis())
                renderPass = System.currentTimeMillis() + 5000
        }
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        if (items == null && renderPass <= System.currentTimeMillis() && !hasDrawn) {
            if (!isLoading) {
                isLoading = true
                executeAsync {
                    try {
                        val background =
                            backgroundHolders.getFor(page = mapData!!.page, pageCount = mapData!!.pageCount)
                                .retrieveRenderedImageBlocking()
                        val mapData = mapData
                        if (background == null || mapData == null) {
                            isLoading = false
                            return@executeAsync
                        }

                        mapCanvas.drawImage(0, 0, background)

                        val font = MinecraftFont.Font

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
                            mapData.subtitle?.let { subtitle ->
                                if (font.isValid(subtitle))
                                    mapCanvas.drawText(
                                        (64 - (font.getWidth(subtitle) * 0.5)).toInt(), 15, font,
                                        "§${MapPalette.matchColor(Color.decode(mapData.subtitleColor))};$subtitle"
                                    )
                            }

                        val items =
                            MainRepositoryProvider.achievementProgressRepository.getTopCounters(
                                mapData.achievementId,
                                floor(((128L * mapData.pageCount) - 35.0) / 22.0).toLong()
                            )
//                        Logger.info("Top 3 for ${mapData.achievementId} = ${items.size}")

                        val yOffset = -128 * mapData.page
                        for ((i, rewardedAchievement) in items.withIndex()) {
                            val count = (mapData.prefix ?: "") + "${rewardedAchievement.count}" + (mapData.suffix
                                ?: "")
                            val name = rewardedAchievement.uuid?.toName()

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
                        this.items = items
                        renderPass = System.currentTimeMillis() + 5000
                        hasDrawn = true
                    } catch (e: Exception) {
                        Logger.capture(e)
                    }

                    isLoading = false
                }
            }
        }
    }

    @JsonClass(generateAdapter = true)
    open class Mapdata : RideScoreboardRenderer.RideScoreMapdata() {
        @Json(name = "achievement_id")
        var achievementId: String = ""
    }

}