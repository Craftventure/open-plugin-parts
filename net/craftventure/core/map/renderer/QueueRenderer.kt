package net.craftventure.core.map.renderer

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.drawHorizontalLine
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.map.fonts.MinguaranaFont
import net.craftventure.core.ride.RideInstance
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.trackedride.TrackedRideManager
import net.craftventure.core.ride.trackedride.TracklessRideManager
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


class QueueRenderer : MapEntryRenderer() {
    private var bufferedImage: BufferedImage? = null
    private var clearImage: BufferedImage? = null
    private var hasDrawn = false
    private var isLoading = false
    private var renderPass = 0L
    private var hasCleared = false

    override val key: String get() = "${imageData?.ride}"

    private var imageData: QueueRenderData? = null
        get() {
            if (field == null) {
                field = try {
                    if (mapEntry?.data != null) {
                        val value = CvMoshi.adapter(QueueRenderData::class.java).fromJson(mapEntry!!.data!!)
                        value
                    } else {
                        null
                    }
                    //                    Logger.console("Converting ${mapEntry.data} to ${value?.name} ${value?.backgroundName}")
                } catch (e: Exception) {
                    null
                }
            }
            return field
        }

    inner class Listener : RideQueue.QueueListener {
        override fun onSizeChanged(queue: RideQueue) {
            hasDrawn = false
            renderPass = System.currentTimeMillis()
//            Logger.debug("onSizeChanged for ${imageData?.ride}")
        }

        override fun onActiveChanged(queue: RideQueue) {
            hasDrawn = false
            renderPass = System.currentTimeMillis()
//            Logger.debug("onActiveChanged for ${imageData?.ride}")
        }
    }

    private var listener = Listener()
    private var queue: RideQueue? = null
        set(value) {
            field?.removeListener(listener)
            field = value
            value?.addListener(listener)
        }
    private var ride: RideInstance? = null

    private fun getRideInstance(): RideInstance? =
        if (ride != null) ride
        else if (imageData != null)
            TrackedRideManager.getTrackedRideList().firstOrNull {
                it.name == imageData?.ride
            }?.also {
                ride = it
            } ?: TracklessRideManager.getRideList().firstOrNull {
                it.id == imageData?.ride
            }?.also {
                ride = it
            }
        else null

    private fun getQueue(): RideQueue? =
        if (queue != null && queue?.ride?.id == imageData?.ride) queue
        else if (imageData != null)
            getRideInstance()?.getQueues()?.let { it.firstOrNull { it.id == imageData?.queueId } }?.also { queue = it }
        else null

    private fun resetQueueInstance() {
        queue?.removeListener(listener)
        queue = null
    }

    fun invalidateForQueue(rideId: String) {
//        Logger.info("Invalidating minigamescore $gameId for data ${mapData?.name}? ${mapData?.name == gameId}")
        if (imageData?.ride == rideId) {
//            invalidate()
            hasDrawn = false
            if (renderPass <= 0)
                renderPass = System.currentTimeMillis() + 5000
        }
    }

    override fun invalidate() {
//        Logger.info("Invalidate queue renderer for ${imageData?.ride}")
//        IllegalStateException().printStackTrace()
        hasDrawn = false
        bufferedImage = null
        imageData = null
    }

    override fun stop() {
        super.stop()
        resetQueueInstance()
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        if (renderPass <= System.currentTimeMillis() && !hasDrawn) {
            if (!isLoading) {
                isLoading = true
                executeAsync {
                    try {
//                        Logger.debug("Rendering queue map for ${imageData?.ride}")
                        val imageData = this@QueueRenderer.imageData
//                        Logger.info("Rendering ${imageData?.name} for ${mapEntry.mapId}")
                        if (imageData == null) {
                            isLoading = false
//                            Logger.debug("No imagedata for ${imageData?.ride}")
                            return@executeAsync
                        }
//                            Logger.console("Loading maxifoto")
                        if (clearImage == null) {
                            val mapFile =
                                File(
                                    CraftventureCore.getInstance().dataFolder,
                                    "data/maps/" + imageData.background_clear
                                )
                            if (mapFile.exists()) {
                                clearImage = ImageIO.read(mapFile)
                            }
                        }
                        if (bufferedImage == null) {
                            val mapFile =
                                File(CraftventureCore.getInstance().dataFolder, "data/maps/" + imageData.background)
                            if (mapFile.exists()) {
                                bufferedImage = ImageIO.read(mapFile)
                            } else {
                                renderPass = 50
                                Logger.warn("Failed to render image for ${mapEntry?.name} to map ${mapEntry?.mapId}")
                            }
                        }
                        val queue = getQueue()

                        if (queue == null) {
//                            Logger.debug("No queue for ${imageData.ride}")
                            hasDrawn = false
                            isLoading = false
                            renderPass = System.currentTimeMillis() + 5000
                            return@executeAsync
                        }
//                        Logger.debug("queue active=${queue.active} wait=${queue.getCurrentEstimateMax()} ride=${ride?.id}/${ride?.getQueues()?.size}")
                        if (!imageData.forceRender && !queue.isActive) {
//                            Logger.debug("No queue active for ${imageData.ride}")
                            if (clearImage != null && !hasCleared)
                                mapCanvas.drawImage(0, 0, clearImage!!)
                            else if (clearImage == null && !hasCleared)
                                for (i in 0 until 128)
                                    mapCanvas.drawHorizontalLine(0, 128, 64 + 2 + i, MapPalette.TRANSPARENT)
                            hasDrawn = true
                            hasCleared = true
                            isLoading = false
                            return@executeAsync
                        }
                        hasCleared = false
//                        Logger.debug("Rendering actual queue for ${imageData.ride}")

                        if (bufferedImage != null) {
                            mapCanvas.drawImage(0, 0, bufferedImage!!)
                        }

//                        mapCanvas.drawText(10, 10, MinguaranaFont.FONT, "Rivers of Ouzo")
//                        mapCanvas.drawText(10, 30, MinguaranaFont.FONT, "5 minutes")
//                        mapCanvas.drawText(10, 50, MinguaranaFont.FONT, "5m20s")
//                        mapCanvas.drawText(10, 70, MinguaranaFont.FONT, "Craftventure")


                        val rideFont = MinguaranaFont.FONT
                        val queueText = if (queue.isActive)
                            queue.getCurrentEstimateMax()?.let { DateUtils.format(it * 1000L, "?") } ?: ""
                        else "No queue"

                        val rideName = getRideInstance()?.ride?.displayName
                        if (rideName != null && rideFont.isValid(rideName))
                            mapCanvas.drawText(
                                (64 - (rideFont.getWidth(rideName) * 0.5)).toInt(), 64 - rideFont.height - 2, rideFont,
                                "§${MapPalette.matchColor(Color.decode(imageData.ride_color))};$rideName"
                            )

                        val queueFont = MinecraftFont.Font
                        if (queueFont.isValid("Wait time")) {
                            mapCanvas.drawText(
                                (64 - (queueFont.getWidth("Wait time") * 0.5)).toInt(), 64 + 2, queueFont,
                                "§${MapPalette.matchColor(Color.decode(imageData.queue_color))};Wait time"
                            )
                        }

                        if (queueFont.isValid(queueText)) {
                            mapCanvas.drawText(
                                (64 - (queueFont.getWidth(queueText) * 0.5)).toInt(),
                                64 + 2 + queueFont.height + 2,
                                queueFont,
                                "§${MapPalette.matchColor(Color.decode(imageData.queue_color))};$queueText"
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
    data class QueueRenderData(
        var ride: String,
        var queueId: String = "main",
        var background_clear: String? = "highscore/clear.png",
        var background: String? = "highscore/background_single.png",
        var queue_color: String = defaultScoreColor,
        var ride_color: String = defaultNameColor,
        var forceRender: Boolean = false,
    )
}
