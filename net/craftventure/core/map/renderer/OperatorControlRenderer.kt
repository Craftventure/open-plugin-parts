package net.craftventure.core.map.renderer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.chat.bungee.extension.asPlainText
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.map.CoordTranslator
import net.craftventure.core.ride.operator.controls.*
import net.craftventure.core.ride.trackedride.RideTrain
import net.craftventure.core.ride.trackedride.TrackSegment
import net.craftventure.core.ride.trackedride.TrackedRideManager
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.utils.LookAtUtil
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.map.*
import org.bukkit.util.Vector


class OperatorControlRenderer : MapEntryRenderer(), InteractableRenderer {
    private val defaultFont = MinecraftFont.Font
    private var cursors = MapCursorCollection()

    //    private var cachedSegments = listOf<TrackSegment>()
    private var lastUpdate = System.currentTimeMillis()

    private var hasDrawnBase = false
    private val trainCursors = HashMap<RideTrain, MapCursor>()
    private val lastTrainUpdate = 0L
    private var worldMapRenderer: WorldMapRendererTask? = null

    override val key: String get() = "${operatorData?.ride}"

    private fun clearTrainCursors() {
        trainCursors.entries.forEach { cursors.removeCursor(it.value) }
        trainCursors.clear()
    }

    private var operatorData: OperatorData? = null
        get() {
            if (field == null) {
                try {
                    field = CvMoshi.adapter(OperatorData::class.java).fromJson(mapEntry!!.data!!)
//                    Logger.console("Converting ${mapEntry.data} to ${value?.name} ${value?.backgroundName}")
                } catch (e: Exception) {
                    field = OperatorData()
                }
            }
            return field
        }

    private var trackedRide: OperableCoasterTrackedRide? = null
        set(value) {
            field = value
            worldCordTranslator = null
            clearTrainCursors()
        }
        get() {
            if (field == null) {
                field = TrackedRideManager.getTrackedRide(operatorData?.ride) as? OperableCoasterTrackedRide
            }
            return field
        }

    private var controls: List<CachedControl>? = null
        get() {
            if (field == null && operatorData?.showSegments == true) {
                field = trackedRide?.provideControls()?.filter { control ->
                    return@filter operatorData?.let {
                        it.segments.isEmpty() || control.owner !is TrackSegment || (control.owner is TrackSegment && it.segments.contains(
                            (control.owner as? TrackSegment)!!.id
                        ))
                    } ?: false
                }?.map { CachedControl(it) }?.sortedBy { it.control.sort }
            }
            return field
        }

    private var blocksections: List<CachedSegment>? = null
        get() {
            if (field == null && operatorData?.showBlocksections == true) {
                field = trackedRide?.trackSegments?.filter {
                    (operatorData?.showNonBlocksections ?: false) || it.isBlockSection
                }?.filter {
                    !it.isBlockSection || it.blockType != TrackSegment.BlockType.CONTINUOUS
                }?.map { CachedSegment(it) }
            }
            return field
        }

    private var worldCordTranslator: CoordTranslator.WorldCoordTranslator? = null
        get() {
            if (field == null) {
                worldCordTranslator = trackedRide?.let {
                    CoordTranslator.WorldCoordTranslator.fromTrackedRide(
                        it,
                        operatorData!!.mapPadding,
                        operatorData!!.horizontalMapBias,
                        operatorData!!.verticalMapBias
                    )
                }
            }
            return field
        }

    private var shouldRender = true

    init {
        reloadData()
    }

    private fun reloadData() {
        executeAsync {
            val data = operatorData ?: return@executeAsync
            val segments = mutableListOf<TrackSegment>()

            for (segmentId in data.segments) {
//                if (data.segments.isEmpty() || data.segments.contains(segmentId)) {
                val segment = trackedRide?.getSegmentById(segmentId) ?: continue
                segments.add(segment)
//                }
            }

//            this.cachedSegments = segments

            shouldRender = true
        }
    }

    override fun invalidate() {
//        Logger.info("Invalidate operator map ${mapEntry.mapId}")
//        IllegalStateException().printStackTrace()
        operatorData = null
        trackedRide = null
        controls = null
        shouldRender = true
        hasDrawnBase = false
        blocksections = null
        worldMapRenderer = null
        cursors = MapCursorCollection()
        reloadData()
    }

    override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player, mapEntry: MapEntry?) {
        val ride = trackedRide ?: return
        val operatorData = this.operatorData ?: return

        if (controls?.any { it.control.lastUpdate > lastUpdate } == true) {
//            logcat { "Map ${mapView.id} ${operatorData.ride} #1" }
            shouldRender = true
        }

        if (blocksections?.any { it.mainCursor?.type != it.getMapCursorType() } == true) {
//            logcat { "Map ${mapView.id} ${operatorData.ride} #2" }
            shouldRender = true
        }

        if (operatorData.blocksectionsMapMode) {
            val now = System.currentTimeMillis()

            if (now > lastTrainUpdate + 1000) {
                worldCordTranslator?.let { translator ->
                    clearTrainCursors()
                    trackedRide?.rideTrains?.forEachIndexed { index, rideTrain ->
                        val car = rideTrain.cars[0]
                        val location = car.location
                        val cursor = MapCursor(
                            CoordTranslator.xCoordToCursorCoord(translator.getMapX(location.x).toInt()),
                            CoordTranslator.yCoordToCursorCoord(translator.getMapZ(location.z).toInt()),
                            CoordTranslator.yawToCursorRotation(Math.toDegrees(car.yawRadian)),
                            MapCursor.Type.BANNER_WHITE, true,
                            "Train ${index + 1}"
                        )
                        trainCursors[rideTrain] = cursor
                        cursors.addCursor(cursor)
                    }
                    mapCanvas.cursors = cursors
                }
            }
        }

        if (!shouldRender)
            return
        shouldRender = false
        lastUpdate = System.currentTimeMillis()

//        Logger.info("Render operator map")
        val backgroundColor = MapPalette.matchColor(java.awt.Color.decode(operatorData.backColor))
        val trackColor = MapPalette.matchColor(java.awt.Color.decode(operatorData.trackColor))

        if (!hasDrawnBase) {
            if (worldMapRenderer?.image == null)
                for (x in 0 until 128) {
                    for (y in 0 until 128) {
                        mapCanvas.setPixel(x, y, backgroundColor)
                    }
                }
            worldMapRenderer?.image?.let {
                //                Logger.debug("Drawing map")
                mapCanvas.drawImage(0, 0, it)
            }
            if (worldMapRenderer == null && operatorData.renderBackgroundMap) {
                worldCordTranslator?.let { translator ->
//                    logcat { "Map ${mapView.id} ${operatorData.ride} Rendering" }
                    worldMapRenderer =
                        WorldMapRendererTask(
                            translator,
                            operatorData.mapRenderMinY,
                            operatorData.mapRenderMaxY,
                            operatorData.mapRenderAlpha
                        ) {
//                            logcat { "Map ${mapView.id} ${operatorData.ride} Invalidate" }
                            shouldRender = true
                            hasDrawnBase = false
                        }.also {
                            it.start(this)
                        }
                }
            }
        }
//        logcat { "Map ${mapView.id} ${operatorData.ride} Render" }

        val name = operatorData.displayName ?: trackedRide?.ride?.displayName ?: operatorData.ride ?: "?"

        var y = 16 + yOffset()
        var controlsUpdated = false
        controls?.let { controls ->
            for (cachedControl in controls) {
                val cursor = cachedControl.mainCursor
                var newCursor: MapCursor? = cursor
                val control = cachedControl.control

                val cursorX = CoordTranslator.xCoordToCursorCoord(8)
                val cursorY = CoordTranslator.yCoordToCursorCoord(y + 4)

//                if (!hasDrawnBase)
//                    for (cX in -2 until 2)
//                        for (cY in -2 until 2)
//                            mapCanvas.setPixel(cX + 8, cY + y + 4, MapPalette.RED)

                when (control) {
                    is OperatorLed -> {
                        if (control.color == ControlColors.GREEN) {
                            if (cursor?.type != MapCursor.Type.BANNER_GREEN)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_GREEN, true)
                        } else if (control.color == ControlColors.RED) {
                            if (cursor?.type != MapCursor.Type.BANNER_RED)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_RED, true)
                        } else if (!control.isEnabled) {
                            if (cursor?.type != MapCursor.Type.BANNER_BLACK)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_BLACK, true)
                        } else {
                            if (cursor?.type != MapCursor.Type.BANNER_RED)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_RED, true)
                        }
                    }

                    is OperatorSwitch -> {
                        if (!control.isEnabled) {
                            if (cursor?.type != MapCursor.Type.BANNER_WHITE)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_WHITE, true)
                        } else if (control.isOn && control.onData == ControlColors.GREEN) {
                            if (cursor?.type != MapCursor.Type.BANNER_GREEN)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_GREEN, true)
                        } else if (control.isOn && control.onData == ControlColors.RED) {
                            if (cursor?.type != MapCursor.Type.BANNER_RED)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_RED, true)
                        } else if (!control.isOn && control.offData == ControlColors.GREEN) {
                            if (cursor?.type != MapCursor.Type.BANNER_GREEN)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_GREEN, true)
                        } else if (!control.isOn && control.offData == ControlColors.RED) {
                            if (cursor?.type != MapCursor.Type.BANNER_RED)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_RED, true)
                        } else if (!control.isOn) {
                            if (cursor?.type != MapCursor.Type.BANNER_GREEN)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_GREEN, true)
                        } else if (control.isOn) {
                            if (cursor?.type != MapCursor.Type.BANNER_RED)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_RED, true)
                        } else if (!control.isOn) {
                            if (cursor?.type != MapCursor.Type.BANNER_GREEN)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_GREEN, true)
                        } else {
                            if (cursor?.type != MapCursor.Type.BANNER_RED)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_RED, true)
                        }
                    }

                    is OperatorButton -> {
                        if (!control.isEnabled) {
                            if (cursor?.type != MapCursor.Type.BANNER_WHITE)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_WHITE, true)
                        } else if (control.isFlashing) {
                            if (cursor?.type != MapCursor.Type.BANNER_GREEN)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_GREEN, true)
                        } else {
                            if (cursor?.type != MapCursor.Type.BANNER_RED)
                                newCursor = MapCursor(cursorX, cursorY, 4, MapCursor.Type.BANNER_RED, true)
                        }
                    }
                }

                if (newCursor !== cursor) {
                    controlsUpdated = true
                    if (cursor != null)
                        cursors.removeCursor(cursor)
                    cursors.addCursor(newCursor!!)
                    cachedControl.mainCursor = newCursor
                }

                if (!hasDrawnBase) {
//                Logger.info("Drawing '${control.name}' ${defaultFont.isValid(control.name)}")
//                try {
                    mapCanvas.drawText(
                        16,
                        y,
                        defaultFont,
                        PlainTextComponentSerializer.plainText().serialize(control.name)
                            .let { if (defaultFont.isValid(it)) it else control.id }
                    )
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
                }
                y += 10
            }
        }

        if (!hasDrawnBase && operatorData.blocksectionsMapMode) {
            val segments = ArrayList(trackedRide!!.trackSegments)
            val min = Vector()
            segments.first().getPosition(0.0, min)
            val max = Vector()
            segments.first().getPosition(0.0, max)

            val calculations = Vector()
            val calculations2 = Vector()
            val yawPitch = LookAtUtil.YawPitch()

            for (subsegments in segments.map { it.subsegments })
                segments.addAll(subsegments)

            for (segment in segments) {
                for (i in 0..Math.floor(segment.length).toInt() * 2) {
                    segment.getPosition(i.toDouble() / 2.0, calculations)
                    min.x = Math.min(min.x, calculations.x)
//                    min.y = Math.min(min.y, calculations.y)
                    min.z = Math.min(min.z, calculations.z)

                    max.x = Math.max(max.x, calculations.x)
//                    max.y = Math.max(min.y, calculations.y)
                    max.z = Math.max(max.z, calculations.z)
                }
            }

            worldCordTranslator?.let { translator ->
//                val segmentNameColor = MapPalette.matchColor(java.awt.Color.decode(operatorData.segmentNameColor))
                for (segment in segments) {
                    val cachedSection = blocksections?.firstOrNull { it.segment === segment }
                    if (cachedSection != null) {
                        segment.getPosition(segment.length / 2.0, calculations)
                        segment.getPosition((segment.length / 2.0) + 0.5, calculations2)

                        LookAtUtil.getYawPitchFromRadian(calculations, calculations2, yawPitch)
                        cachedSection.rotation = CoordTranslator.yawToCursorRotation(Math.toDegrees(yawPitch.yaw))

                        cachedSection.x = translator.getMapX(calculations.x).toInt()
                        cachedSection.z = translator.getMapZ(calculations.z).toInt()
                    }

                    for (i in 0..Math.floor(segment.length).toInt() * 2) {
                        segment.getPosition(i.toDouble() / 2.0, calculations)
                        mapCanvas.setPixel(
                            translator.getMapX(calculations.x).toInt(),
                            translator.getMapZ(calculations.z).toInt(), trackColor
                        )
                    }
                }
            }
        }

        blocksections?.let {
            for (cachedBlocksection in it) {
                val cursor = cachedBlocksection.mainCursor
                var newCursor: MapCursor? = cursor
                val segment = cachedBlocksection.segment

                val cursorX = CoordTranslator.xCoordToCursorCoord(cachedBlocksection.x ?: 8)
                val cursorY = CoordTranslator.yCoordToCursorCoord(cachedBlocksection.z ?: y + 4)

//                if (!hasDrawnBase && segment.isBlockSection)
//                    for (cX in -2 until 2)
//                        for (cY in -2 until 2)
//                            mapCanvas.setPixel(
//                                cX + (cachedBlocksection.x ?: 8),
//                                cY + (cachedBlocksection.z ?: y + 4), MapPalette.RED
//                            )

                val type = cachedBlocksection.getMapCursorType()

                if (cursor?.type != type) {
                    newCursor =
                        MapCursor(
                            cursorX,
                            cursorY,
                            cachedBlocksection.rotation ?: 4,
                            type,
                            true,
                            if (operatorData.showBlocksectionNames) segment.nameOnMap else null
                        )
                }

                if (newCursor !== cursor) {
                    controlsUpdated = true
                    if (cursor != null)
                        cursors.removeCursor(cursor)
                    cursors.addCursor(newCursor!!)
                    cachedBlocksection.mainCursor = newCursor
                }

                if (!hasDrawnBase && !operatorData.blocksectionsMapMode) {
//                Logger.info("Drawing '${control.name}' ${defaultFont.isValid(control.name)}")
//                try {
                    mapCanvas.drawText(
                        16,
                        y,
                        defaultFont,
                        segment.displayName.asPlainText(),
                    )
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
                }
                y += 10
            }
        }

        if (!hasDrawnBase)
            if (defaultFont.isValid(name)) {

                val displayNameColor = MapPalette.matchColor(java.awt.Color.decode(operatorData.displayNameColor))
                mapCanvas.drawText(
                    Math.max((64 - (defaultFont.getWidth(name) * 0.5)).toInt(), 0),
                    2 + yOffset(),
                    defaultFont,
                    "§$displayNameColor;$name"
                )
            }
//        if (mapCanvas.cursors !== cursors) {

        if (controlsUpdated) {
//            Logger.info("Controls updated")
            mapCanvas.cursors = cursors
        }

        val map = Bukkit.getMap(mapView.id)
        for (o in 0..ride.totalOperatorSpots) {
            ride.getOperatorForSlot(o)?.let {
                if (map != null)
                    it.sendMap(map)
            }
        }
//        }
        hasDrawnBase = true
    }

    private fun yOffset() = if (operatorData?.blocksectionsMapMode == true)
        0
    else
        (((128 - 26) - (((blocksections?.size ?: 0) + (controls?.size ?: 0)) * 10.0)) / 2.0).toInt()

    override fun interact(player: Player, mapId: Int, x: Double, y: Double) {
        val clickY = 128 * y
//        Logger.info("Operator map clicked by ${player.name} at ${x.format(2)} ${y.format(2)}")

        var controlY = 16 + yOffset()
        val trackedRide = trackedRide ?: return
        val controls = controls ?: return
        for (cachedControl in controls) {
            if (clickY >= controlY && clickY < controlY + 10) {
//                Logger.info("Control ${cachedControl.control.id} clicked")
                executeSync {
                    cachedControl.control.click(trackedRide, player)
                }
                return
            }
            controlY += 10
        }
    }

    @JsonClass(generateAdapter = true)
    class OperatorData {
        @Json(name = "map_padding")
        var mapPadding: Int = 2

        @Json(name = "display_name_color")
        var displayNameColor: String = "#a0a0a0"

        @Json(name = "segment_name_color")
        var segmentNameColor: String = "#a0a0a0"

        @Json(name = "track_color")
        var trackColor: String = "#a0a0a0"

        @Json(name = "back_color")
        var backColor: String = "#d1d1d1"
        var ride: String? = null

        @Json(name = "display_name")
        var displayName: String? = null
        var segments: List<String> = emptyList()

        @Json(name = "show_segments")
        var showSegments: Boolean = false

        @Json(name = "render_background_map")
        var renderBackgroundMap: Boolean = false

        @Json(name = "map_render_min_y")
        var mapRenderMinY: Int = 0

        @Json(name = "map_render_max_y")
        var mapRenderMaxY: Int = 1024

        @Json(name = "map_render_alpha")
        var mapRenderAlpha: Double = 1.0

        @Json(name = "show_blocksection_names")
        var showBlocksectionNames: Boolean = false

        @Json(name = "show_blocksections")
        var showBlocksections: Boolean = false

        @Json(name = "show_non_blocksections")
        var showNonBlocksections: Boolean = false

        @Json(name = "blocksections_map_mode")
        var blocksectionsMapMode: Boolean = false

        @Json(name = "horizontal_map_bias")
        var horizontalMapBias: Double = 0.5

        @Json(name = "vertical_map_bias")
        var verticalMapBias: Double = 0.5
    }

    class CachedSegment(
        val segment: TrackSegment,
        var mainCursor: MapCursor? = null,
        var x: Int? = null,
        var z: Int? = null,
        var rotation: Byte? = null
    ) {
        fun getMapCursorType(): MapCursor.Type {
            val isBlocking = segment.isBlocked
//            val hasTrain = segment.isContainsTrain
//            val blocksection = segment.isBlockSection

//            if (hasTrain)
//                return MapCursor.Type.GREEN_POINTER
            if (isBlocking) {
//                if (segment is StationSegment)
//                    return MapCursor.Type.MANSION
//                if (segment is SidewaysTransferSegment.TransferSegment)
//                    return MapCursor.Type.TEMPLE
                return MapCursor.Type.BANNER_RED
            }
//            if (!blocksection)
//                return MapCursor.Type.BANNER_WHITE

            return MapCursor.Type.BANNER_GREEN
        }
    }

    class CachedControl(
        val control: OperatorControl,
        var mainCursor: MapCursor? = null
    )
}
