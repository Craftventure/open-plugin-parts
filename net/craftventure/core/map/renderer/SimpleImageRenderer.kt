package net.craftventure.core.map.renderer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapView
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


class SimpleImageRenderer : MapEntryRenderer() {
    private var bufferedImage: BufferedImage? = null
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L

    override val key: String get() = "${imageData?.name}"

    private var imageData: SimpleImageData? = null
        get() {
            if (field == null) {
                try {
                    field = CvMoshi.adapter(SimpleImageData::class.java).fromJson(mapEntry!!.data!!)
//                    Logger.console("Converting ${mapEntry.data} to ${value?.name} ${value?.backgroundName}")
                } catch (e: Exception) {
                    field = SimpleImageData().apply { name = mapEntry?.data }
                }
            }
            return field
        }

    override fun invalidate() {
//        Logger.info("Invalidate ${imageData?.name} for ${mapEntry.mapId}")
//        IllegalStateException().printStackTrace()
        hasDrawn = false
        bufferedImage = null
        imageData = null
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        if (bufferedImage == null && renderPass <= System.currentTimeMillis() && !hasDrawn) {
            if (!isLoading) {
                isLoading = true
                executeAsync {
                    try {
                        val imageData = this@SimpleImageRenderer.imageData
//                        Logger.info("Rendering ${imageData?.name} for ${mapEntry.mapId}")
                        if (imageData == null) {
                            isLoading = false
                            return@executeAsync
                        }
//                            Logger.console("Loading maxifoto")
                        val mapFile = File(CraftventureCore.getInstance().dataFolder, "data/maps/" + imageData.name)
                        if (mapFile.exists()) {
                            bufferedImage = ImageIO.read(mapFile)
                            mapCanvas.drawImage(imageData.xOffset, imageData.yOffset, bufferedImage!!)
                        } else {
                            renderPass = 50
                            Logger.warn(
                                "Failed to render image for %s to map %s",
                                logToCrew = false,
                                params = *arrayOf(mapEntry!!.name, mapEntry.mapId)
                            )
                        }
//                            val start = System.currentTimeMillis()
//                            Logger.console("Drawing took %s for %s (%s) (target=%s)",
//                                    (System.currentTimeMillis() - start),
//                                    mapEntry.mapId,
//                                    mapEntry.name,
//                                    mapCanvas.javaClass)
                        hasDrawn = true
                    } catch (e: Exception) {
                        Logger.capture(e)
                    }

                    isLoading = false
                }
            }
        }
//        if (bufferedImage != null && !hasDrawn) {
//            val start = System.currentTimeMillis()
//            mapCanvas.drawImage(0, 0, bufferedImage)
//            Logger.console("Drawing took %s for %s (%s)", (System.currentTimeMillis() - start), mapEntry.mapId, mapEntry.name)
//            hasDrawn = true
//        }
    }

    @JsonClass(generateAdapter = true)
    class SimpleImageData(
        var name: String? = null,
        @Json(name = "x_offset")
        var xOffset: Int = 0,
        @Json(name = "y_offset")
        var yOffset: Int = 0
    )
}
