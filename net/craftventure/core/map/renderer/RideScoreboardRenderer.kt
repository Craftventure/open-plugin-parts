package net.craftventure.core.map.renderer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.drawHorizontalLine
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import net.craftventure.database.generated.cvdata.tables.pojos.RideCounter
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapPalette
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import java.awt.Color
import kotlin.math.floor


class RideScoreboardRenderer : MapEntryRenderer() {
    private var sortedRideCounters: List<RideCounter>? = null
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L

    override val key: String get() = "${mapData?.name}"

    private var mapData: RideScoreMapdata? = null
        get() {
            if (field == null) {
                try {
                    field = CvMoshi.adapter(RideScoreMapdata::class.java).fromJson(mapEntry!!.data!!)
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
        sortedRideCounters = null
        mapData = null
    }

    fun invalideForRide(rideId: String) {
        if (mapData?.name == rideId) {
            invalidate()
            if (renderPass <= 0)
                renderPass = System.currentTimeMillis() + 5000
        }
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        if (sortedRideCounters == null && renderPass <= System.currentTimeMillis() && !hasDrawn) {
            if (!isLoading) {
                isLoading = true
                executeAsync {
                    try {
//                        logcat { "Reloading ridescores ${mapData?.name}" }
                        val background =
                            backgroundHolders.getFor(page = mapData!!.page, pageCount = mapData!!.pageCount)
                                .retrieveRenderedImageBlocking()
                        val mapData = mapData
                        if (background == null || mapData == null) {
//                            logcat { "background=$background mapData=$mapData" }
                            isLoading = false
                            return@executeAsync
                        }
//                        logcat { "Drawing ridescores ${mapData.name}" }

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

                        val items = MainRepositoryProvider.rideCounterRepository
                            .getTopCounters(
                                mapData.name!!,
                                floor(((128L * mapData.pageCount) - 35.0) / 22.0).toLong()
                            )
                        val yOffset = -128 * mapData.page
                        for ((i, rideCounter) in items.withIndex()) {
                            val count =
                                "#${i + 1} " + (mapData.prefix ?: "") + "${rideCounter.count}" + (mapData.suffix ?: "")
                            val name = rideCounter.uuid!!.toName()

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
                            if (font.isValid(name))
                                mapCanvas.drawText(
                                    (64 - (font.getWidth(name) * 0.5)).toInt(), yOffset + 34 + (i * 22) + 10, font,
                                    "§${MapPalette.matchColor(Color.decode(mapData.nameColor))};$name"
                                )
//                            else
//                                Logger.console("Invalid map text '$count'")
                        }
                        this.sortedRideCounters = items
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
    open class RideScoreMapdata {
        var name: String? = null

        @Json(name = "background_image_prefix")
        var backgroundImagePrefix: String? = "highscore/background"

        @Json(name = "score_color")
        var scoreColor = defaultScoreColor

        @Json(name = "name_color")
        var nameColor = defaultNameColor
        var title: String? = null
        var subtitle: String? = null

        @Json(name = "divider_color")
        var dividerColor = defaultDividerColor

        @Json(name = "title_color")
        var titleColor = defaultTitleColor

        @Json(name = "subtitle_color")
        var subtitleColor = defaultSubtitleColor
        var prefix: String? = ""
        var suffix: String? = ""

        @Json(name = "divider_start_x")
        var dividerStartX: Int = 20

        @Json(name = "divider_end_x")
        var dividerEndX: Int = 128 - 20
        var page: Int = 0

        @Json(name = "page_count")
        var pageCount: Int = 1
    }

}