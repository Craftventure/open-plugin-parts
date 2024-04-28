package net.craftventure.core.map.renderer

import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView

abstract class MapEntryRenderer @JvmOverloads protected constructor(
    contextual: Boolean = false
) : MapRenderer(contextual) {
    abstract val key: String

    var mapEntry: MapEntry? = null
        set(value) {
            field = value
            invalidate()
        }

    abstract fun invalidate()

    open fun stop() {}

    abstract fun render(
        mapView: MapView,
        mapCanvas: MapCanvas,
        player: Player,
        mapEntry: MapEntry?
    )

    final override fun render(
        mapView: MapView,
        mapCanvas: MapCanvas,
        player: Player
    ) {
        try {
            render(mapView, mapCanvas, player, mapEntry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val defaultDividerColor: String = "#e3ded3"
        const val defaultTitleColor: String = "#ba8524"
        const val defaultSubtitleColor: String = "#ba8524"
        const val defaultScoreColor: String = "#9f741d"
        const val defaultNameColor: String = "#634e24"
    }
}