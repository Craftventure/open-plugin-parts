package net.craftventure.core.map.renderer

import kotlinx.coroutines.*
import net.craftventure.core.async.CvDispatchers
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapView

abstract class CoroutineMapEntryRenderer @JvmOverloads protected constructor(
    contextual: Boolean = false
) : MapEntryRenderer(contextual) {
    private val scope = CoroutineScope(SupervisorJob() + CvDispatchers.mainThreadDispatcher)
    private var job: Job? = null

    override fun stop() {
        super.stop()
        job?.cancel()
        scope.cancel()
    }

    fun isRunning() = job?.isActive == true

    open fun shouldRender(): Boolean = true

    final override fun render(
        mapView: MapView,
        mapCanvas: MapCanvas,
        player: Player,
        mapEntry: MapEntry?
    ) {
        if (!shouldRender()) return

        val currentJob = job
        if (currentJob == null || !currentJob.isActive || currentJob.isCompleted) {
//            logcat { "Launching new render scope" }
            job = scope.launch(Dispatchers.IO) {
                doRender(mapView, mapCanvas, player, mapEntry)
            }
        }
    }

    abstract suspend fun doRender(
        mapView: MapView,
        mapCanvas: MapCanvas,
        player: Player,
        mapEntry: MapEntry?,
    )
}