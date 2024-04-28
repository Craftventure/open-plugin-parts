package net.craftventure.core.map.renderer

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.drawHorizontalLine
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerKeyValue
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapPalette
import org.bukkit.map.MapView
import org.bukkit.map.MinecraftFont
import java.awt.Color


class PlayerKeyValueRenderer : MapEntryRenderer() {
    private var sortedItems: List<PlayerKeyValue>? = null
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L

    override val key: String get() = "${mapData?.name}"

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
    private val backgroundHolders = DefaultBackgroundHolders()

    override fun invalidate() {
        hasDrawn = false
        sortedItems = null
        mapData = null
    }

    fun invalidateForKey(key: String) {
//        Logger.info("${mapData?.name} startsWith $key")
        if (key.startsWith(mapData?.name ?: "")) {
            invalidate()
            if (renderPass <= 0)
                renderPass = System.currentTimeMillis() + 5000
        }
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        if (sortedItems == null && renderPass <= System.currentTimeMillis() && !hasDrawn) {
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

                        val font = MinecraftFont.Font
                        val key = when (mapData.name) {
//                            "wishingwell_2018" -> PlayerKeyValueDatabase.currentWishingWellKey()
                            else -> mapData.name ?: "?"
                        }

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

                        val items = MainRepositoryProvider.playerKeyValueRepository
                            .getHighestValues(key, if (mapData.pageCount >= 2) 10 else 4)

                        val yOffset = -128 * mapData.page
                        for ((i, value) in items.withIndex()) {
                            val valueToDisplay = value.value?.toDoubleOrNull()?.let {
                                if (mapData.multiply != null)
                                    return@let it * mapData.multiply!!
                                return@let it
                            } ?: 0.0
                            val count =
                                "#${i + 1} " + (mapData.prefix ?: "") + (valueToDisplay).format(2) + (mapData.suffix
                                    ?: "")
                            val name = value.uuid?.toName()

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
                        this.sortedItems = items
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
    open class Mapdata : RideScoreboardRenderer.RideScoreMapdata() {
        var multiply: Double? = null
    }

}