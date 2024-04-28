package net.craftventure.core.map.renderer

import com.squareup.moshi.JsonClass
import kotlinx.coroutines.runBlocking
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.map.holder.MatScoreImageHolder
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapView


class MatScoreRenderer : MapEntryRenderer() {
    private var lastRender: Long = 0L
    private var scoreHolder: MatScoreImageHolder? = null
    private var hasDrawn = false
    private var isLoading = false

    override val key: String get() = "score"

    private var imageData: SimpleImageData? = null
        get() {
            if (field == null) {
                try {
                    field = CvMoshi.adapter(SimpleImageData::class.java).fromJson(mapEntry!!.data!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return field
        }

    override fun invalidate() {
        hasDrawn = false
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        if (scoreHolder == null || scoreHolder!!.lastUpdate > lastRender || !hasDrawn) {
            if (!isLoading) {
                isLoading = true
                executeAsync {
//                    Logger.debug("Updating scoreboard")
                    try {
                        val imageData = this@MatScoreRenderer.imageData ?: return@executeAsync

                        if (scoreHolder == null) {
                            scoreHolder =
                                MapManager.instance.getImageHolder("matscore${imageData.team}") as? MatScoreImageHolder
                        }

                        if (scoreHolder == null) {
                            Logger.warn("Failed to render image ${mapEntry?.name} to map ${mapEntry?.mapId}")
                            return@executeAsync
                        }

                        val image = runBlocking { scoreHolder?.retrieveRenderedImage() }
                        if (image != null)
                            executeSync {
                                mapCanvas.drawImage(imageData.xOffset, imageData.yOffset, image)
                            }
                        hasDrawn = true
                        lastRender = System.currentTimeMillis()
                    } catch (e: Exception) {
                        Logger.capture(e)
                    } finally {
//                        Logger.debug("Finally...")
                        isLoading = false
                    }
                }
            }
        }
    }

    @JsonClass(generateAdapter = true)
    class SimpleImageData(
        val xOffset: Int = 0,
        val yOffset: Int = 0,
        val team: Int,
    )
}
