package net.craftventure.audioserver.packet

import com.squareup.moshi.JsonClass
import org.bukkit.util.Vector

@JsonClass(generateAdapter = true)
class PacketPolygonOverlayAdd(
    val mode: Mode = Mode.ADD,
    var group: String?,
    val polygons: Set<AreaOverlay>,
) : BasePacket(PacketID.POLYGON_OVERLAY_ADD) {

    enum class Mode {
        ADD,
        SET
    }

    abstract class AreaOverlay {
        lateinit var id: String
        var title: String? = null
        var group: String? = null

        var stroke: Boolean = false
        var strokeColor: String = "#ffaa00"

        var fill: Boolean = true
        var fillColor: String = "#ffaa00"
        var fillOpacity: Double = 0.2
        var fillRule: String = "evenodd"

        //        var group: String? = null
        var opacity: Double = 1.0
        var zIndex: Double = 1.0

        var className: String? = null
        var interactive: Boolean = false

        constructor()

        constructor(
            id: String,
            title: String?,
            group: String?,
            stroke: Boolean,
            strokeColor: String,
            fill: Boolean,
            fillColor: String,
            fillOpacity: Double,
            fillRule: String,
            opacity: Double,
            className: String?,
            interactive: Boolean
        ) {
            this.id = id
            this.title = title
            this.group = group
            this.stroke = stroke
            this.strokeColor = strokeColor
            this.fill = fill
            this.fillColor = fillColor
            this.fillOpacity = fillOpacity
            this.fillRule = fillRule
            this.opacity = opacity
            this.className = className
            this.interactive = interactive
        }


    }

    @JsonClass(generateAdapter = true)
    data class RectangleOverlay(
        val min: Vector,
        val max: Vector,
    ) : AreaOverlay()
}
