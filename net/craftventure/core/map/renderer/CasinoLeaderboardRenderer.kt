package net.craftventure.core.map.renderer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.CasinoLog
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapPalette
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


class CasinoLeaderboardRenderer : MapEntryRenderer() {
    private var items: List<CasinoLog>? = null
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L
    override val key: String get() = "${mapData?.machineId}"

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
    private var scrollBackground: BufferedImage? = null
        get() {
            if (field == null && mapData != null) {
                val mapFile = File(CraftventureCore.getInstance().dataFolder, "data/maps/${mapData?.backgroundImage}")
                try {
                    field = ImageIO.read(mapFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Logger.warn("Failed to load $mapFile", logToCrew = false)
                }
            }
            return field
        }

    override fun invalidate() {
        hasDrawn = false
        items = null
        scrollBackground = null
        mapData = null
    }

    fun invalidateForMachine(machineId: String) {
//        Logger.debug("Invalidating casino $machineId vs ${mapData?.machineId}")
        if (mapData?.machineId == machineId) {
//            Logger.info("Invalidating casino scoreboard $machineId")
            invalidate()
            if (renderPass <= System.currentTimeMillis())
                renderPass = System.currentTimeMillis() + 5000
        }
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
//        Logger.debug("Checking if needs to render: ${mapData?.machineId}")

        if (items == null && renderPass <= System.currentTimeMillis() && !hasDrawn) {
            if (!isLoading) {
                isLoading = true
                executeAsync {
                    try {
//                        Logger.debug("Rendering ${mapData?.machineId}")
                        val background = scrollBackground
                        val mapData = mapData
                        if (background == null || mapData == null) {
                            isLoading = false
                            return@executeAsync
                        }

                        mapCanvas.drawImage(0, 0, background)

                        val font = MinecraftFont.Font

                        mapData.title?.let { title ->
                            if (font.isValid(title))
                                mapCanvas.drawText(
                                    (64 - (font.getWidth(title) * 0.5)).toInt(), 14, font,
                                    "§${MapPalette.matchColor(Color.decode(mapData.titleColor))};$title"
                                )
                        }
                        mapData.subtitle?.let { subtitle ->
                            if (font.isValid(subtitle))
                                mapCanvas.drawText(
                                    (64 - (font.getWidth(subtitle) * 0.5)).toInt(), 26, font,
                                    "§${MapPalette.matchColor(Color.decode(mapData.subtitleColor))};$subtitle"
                                )
                        }

                        val items =
                            MainRepositoryProvider.casinoLogRepository.getTopRewards(mapData.machineId, 3)
//                        Logger.info("Top 3 for ${mapData.achievementId} = ${items.size}")
                        for ((i, rewardedAchievement) in items.withIndex()) {
                            val count = (mapData.prefix ?: "") + "${rewardedAchievement.vc}" + (mapData.suffix
                                ?: "")
                            val name = rewardedAchievement.uuid?.toName()

                            if (font.isValid(count))
                                mapCanvas.drawText(
                                    (64 - (font.getWidth(count) * 0.5)).toInt(), 44 + (i * 22), font,
                                    "§${MapPalette.matchColor(Color.decode(mapData.scoreColor))};$count"
                                )
                            if (name != null && font.isValid(name))
                                mapCanvas.drawText(
                                    (64 - (font.getWidth(name) * 0.5)).toInt(), 44 + (i * 22) + 10, font,
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
    class Mapdata {
        @Json(name = "machine_id")
        var machineId: String = ""

        @Json(name = "background_image")
        var backgroundImage: String? = "highscore/empty.png"

        @Json(name = "score_color")
        var scoreColor = "#2b1d00"

        @Json(name = "name_color")
        var nameColor = "#674500"
        var title: String? = null
        var subtitle: String? = null

        @Json(name = "title_color")
        var titleColor = "#2b1d00"

        @Json(name = "subtitle_color")
        var subtitleColor = "#674500"
        var prefix: String? = null
        var suffix: String? = "x"
    }

}