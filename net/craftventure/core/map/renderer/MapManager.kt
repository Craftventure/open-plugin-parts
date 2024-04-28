package net.craftventure.core.map.renderer

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withPermit
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.ktx.util.Logger.severe
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.MapInitializeEvent
import org.bukkit.map.MapView
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO

class MapManager private constructor() : Listener {
    private val renderers =
        HashMap<String, Class<out MapEntryRenderer>>()

    private val imageHolders = ConcurrentHashMap<String, ImageHolder>()
    private val imageHolderSemaphore = Semaphore(1)

    fun invalidateAllImageHolders() {
        imageHolders.values.forEach {
            it.invalidateSources()
            it.invalidateRender()
        }
    }

    @JvmOverloads
    fun addImageHolder(imageHolder: ImageHolder, replace: Boolean = true) {
        if (!replace && imageHolders[imageHolder.id] != null) {
            throw IllegalStateException("This ID is already taken")
        }
        imageHolders[imageHolder.id] = imageHolder
    }

    fun getImageHolder(id: String): ImageHolder? = imageHolders[id]

    fun getOrCreateImageHolder(file: File): ImageHolder {
        imageHolderSemaphore.acquire()
        try {
            val path = file.canonicalPath
            getImageHolder(path)?.let { return it }
//        if (!file.exists()) {
//            logcat(priority = LogPriority.WARN, logToCrew = true) { "Failed to find imageholder for ${file.path}" }
//            return emptyImageHolder
//        }

            try {
                synchronized(imageHolders) {
//                val image = ImageIO.read(file)
                    val holder = FileImageHolder(file)
                    addImageHolder(holder)
                    return holder
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return emptyImageHolder
        } finally {
            imageHolderSemaphore.release()
        }
    }

    fun getImageHolder(file: File): ImageHolder = getOrCreateImageHolder(file)

    fun clicked(
        player: Player,
        mapId: Int,
        x: Double,
        y: Double
    ): Boolean {
        var hasUpdatedMap = false
        val mapView = getMapViewById(mapId)
        if (mapView != null) {
            for (mapRenderer in mapView.renderers) {
                if (mapRenderer is InteractableRenderer) {
                    (mapRenderer as InteractableRenderer).interact(player, mapId, x, y)
                    hasUpdatedMap = true
                }
            }
        }
        return hasUpdatedMap && (!player.isCrew() || !player.isSneaking)
    }

    fun putRenderer(renderer: String, handler: Class<out MapEntryRenderer>): MapManager {
        renderers[renderer] = handler
        return this
    }

    fun invalidateRide(rideId: String?) {
        val items =
            MainRepositoryProvider.mapEntriesRepository.cachedItems
        for (i in items.indices) {
            val mapEntry = items[i]
            if (mapEntry.renderer != null) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                if (mapView != null) {
                    for (mapRenderer in mapView.renderers) {
                        if (mapRenderer is RideScoreboardRenderer) {
                            mapRenderer.invalideForRide(rideId!!)
                        }
                    }
                }
            }
        }
    }

    fun invalidatePlayerKeyValueKey(key: String?) {
        val items =
            MainRepositoryProvider.mapEntriesRepository.cachedItems
        for (i in items.indices) {
            val mapEntry = items[i]
            if (mapEntry.renderer != null) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                if (mapView != null) {
                    for (mapRenderer in mapView.renderers) {
                        if (mapRenderer is PlayerKeyValueRenderer) {
                            mapRenderer.invalidateForKey(key!!)
                        }
                    }
                }
            }
        }
    }

    fun invalidateForAchievement(achievementId: String?) {
        val items =
            MainRepositoryProvider.mapEntriesRepository.cachedItems
        for (i in items.indices) {
            val mapEntry = items[i]
            if (mapEntry.renderer != null) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                if (mapView != null) {
                    for (mapRenderer in mapView.renderers) {
                        if (mapRenderer is AchievementScoreboardRenderer) {
                            mapRenderer.invalidateForAchievement(achievementId!!)
                        }
                    }
                }
            }
        }
    }

    fun invalidateForCasinoMachine(casinoMachine: String?) {
        val items =
            MainRepositoryProvider.mapEntriesRepository.cachedItems
        for (i in items.indices) {
            val mapEntry = items[i]
            if (mapEntry.renderer != null) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                if (mapView != null) {
                    for (mapRenderer in mapView.renderers) {
                        if (mapRenderer is CasinoLeaderboardRenderer) {
                            mapRenderer.invalidateForMachine(casinoMachine!!)
                        }
                    }
                }
            }
        }
    }

    fun invalidateGame(gameId: String?) {
        val items =
            MainRepositoryProvider.mapEntriesRepository.cachedItems
        for (i in items.indices) {
            val mapEntry = items[i]
            if (mapEntry.renderer != null) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                if (mapView != null) {
                    for (mapRenderer in mapView.renderers) {
                        if (mapRenderer is MinigameScoreboardRenderer) {
                            mapRenderer.invalidateForGame(gameId!!)
                        }
                    }
                }
            }
        }
    }

    inline fun <reified T : MapEntryRenderer> invalidate(key: String) {
        val items =
            MainRepositoryProvider.mapEntriesRepository.cachedItems
        for (i in items.indices) {
            val mapEntry = items[i]
            if (mapEntry.renderer != null) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                if (mapView != null) {
                    for (mapRenderer in mapView.renderers) {
                        if (mapRenderer is T && mapRenderer.key == key) {
                            mapRenderer.invalidate()
                        }
                    }
                }
            }
        }
    }

    fun updateAllMaps(removeExistingRenderers: Boolean): MapManager {
        val items =
            MainRepositoryProvider.mapEntriesRepository.cachedItems
        for (i in items.indices) {
            val mapEntry = items[i]
            if (removeExistingRenderers) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                if (mapView != null) {
                    while (mapView.renderers.isNotEmpty()) {
                        (mapView.renderers[0] as? MapEntryRenderer)?.stop()
                        mapView.removeRenderer(mapView.renderers[0])
                    }
                }
            }
            updateMap(mapEntry.mapId!!)
        }
        return this
    }

    private fun updateMap(id: Int) {
        if (!updateMap(MainRepositoryProvider.mapEntriesRepository.findCached(id))) {
            severe("Failed to update map %s", false, id)
        }
    }

    fun updateMap(mapEntry: MapEntry?): Boolean {
        if (mapEntry != null) {
            val renderer: MapEntryRenderer? = null
            if (mapEntry.renderer != null) {
                val mapView = getMapViewById(mapEntry.mapId!!)
                mapView?.let { updateMap(it, mapEntry) }
            }
            return true
        } else {
            severe("§eNo valid map given for null mapentry")
        }
        return false
    }

    @JvmOverloads
    fun updateMap(
        mapView: MapView,
        mapEntry: MapEntry? = MainRepositoryProvider.mapEntriesRepository.findCached(mapView.id)
    ): Boolean {
        if (mapEntry == null) return false
        var renderer: MapEntryRenderer? = null
        //        Logger.console("Renderers %s", mapView.getRenderers().size());
        if (mapView.renderers.size == 1 && mapView.renderers[0] is MapEntryRenderer) {
//                        Logger.console("Reusing renderer for %s", mapEntry.getMapId());
            renderer = mapView.renderers[0] as MapEntryRenderer
            renderer.invalidate()
            renderer.stop()
            mapView.removeRenderer(renderer)
        } else {
//                        Logger.console("Creating new renderer for map %s", mapEntry.getMapId());
            for (mapRenderer in mapView.renderers) {
                (mapRenderer as? MapEntryRenderer)?.stop()
                mapView.removeRenderer(mapRenderer)
            }
            mapView.centerX = -10000
            mapView.centerZ = -10000
            mapView.scale = MapView.Scale.CLOSEST
            try {
                val rendererClass = renderers[mapEntry.renderer]
                renderer = rendererClass!!.newInstance()
                //                            mapView.addRenderer(renderer);
            } catch (e: Exception) {
                capture(e)
            }
        }
        if (renderer != null) {
            renderer.mapEntry = mapEntry
            mapView.addRenderer(renderer)
        }
        return true
    }

    fun MapView.getData() = MainRepositoryProvider.mapEntriesRepository.cachedItems.firstOrNull { it.mapId == this.id }

    fun getAllMapViews() =
        MainRepositoryProvider.mapEntriesRepository.cachedItems
            .map { it to getMapViewById(it.mapId!!) }

    inline fun <reified T> getAllMapViewsForRenderer(rendererClass: T) =
        getAllMapViews().filter { it.second?.renderers?.any { it is T } == true }

    fun getMapViewById(id: Int): MapView? {
        var mapView = Bukkit.getMap(id)
        if (mapView == null) {
            while (mapView == null || mapView.id < id) {
                mapView = Bukkit.createMap(Bukkit.getWorld("world")!!)
            }
        } else {
        }
        return mapView
    }

    @EventHandler
    fun onMapInitialize(event: MapInitializeEvent) {
        updateMap(event.map)
        //        event.getMap().getRenderers().clear();
//        event.getMap().setCenterX(-100000);
//        event.getMap().setCenterZ(-100000);
//        event.getMap().setScale(MapView.Scale.CLOSEST);
//        event.getMap().addRenderer(new MapRenderer() {
//            @Override
//            public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
//            }
//        });
    }

    interface ImageHolder {
        val id: String
        val lastUpdate: Long

        fun retrieveRenderedImageBlocking() = runBlocking { retrieveRenderedImage() }
        suspend fun retrieveRenderedImage(): BufferedImage
        fun invalidateSources() {}
        fun invalidateRender() {}
    }

    class PreparedImageHolder(
        override val id: String,
        private val image: BufferedImage,
        override val lastUpdate: Long = System.currentTimeMillis()
    ) : ImageHolder {
        override suspend fun retrieveRenderedImage(): BufferedImage = image
    }

    class FileImageHolder(
        val file: File,
    ) : ImageHolder {
        private val lock = kotlinx.coroutines.sync.Semaphore(1)
        private var cache: BufferedImage? = null
        override val id: String = file.canonicalPath
        override var lastUpdate: Long = 0L

        override fun invalidateSources() {
            super.invalidateSources()
            cache = null
        }

        override suspend fun retrieveRenderedImage(): BufferedImage {
            lock.withPermit {
                if (cache != null) return cache!!

                cache = emptyImageHolder.retrieveRenderedImage()
                if (file.exists())
                    try {
                        cache = ImageIO.read(file)
                        lastUpdate = System.currentTimeMillis()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                return cache ?: emptyImageHolder.retrieveRenderedImage()
            }
        }
    }

    companion object {
        @JvmStatic
        val instance by lazy { MapManager() }

        val emptyImageHolder = PreparedImageHolder("empty", BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
    }

    init {
        Bukkit.getServer().pluginManager.registerEvents(this, CraftventureCore.getInstance())
    }
}