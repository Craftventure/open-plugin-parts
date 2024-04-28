package net.craftventure.core.feature.dressingroom

import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Logger
import java.io.File

object DressingRoomManager {
    private val rooms = mutableSetOf<DressingRoom>()

    fun reload() {
        logcat { "Reloading dressing rooms" }
        // TODO: Add an option to only reload changes
        clearShops()
        loadShops(File(CraftventureCore.getInstance().dataFolder, "data/dressingroom"))
    }

    private fun clearShops() {
        rooms.forEach { it.stop() }
        rooms.clear()
    }

    private fun loadShops(directory: File) {
        val roomFiles = directory.walkTopDown().filter { it.isFile && it.extension == "json" }.toList()
        val adapter = CvMoshi.adapter(DressingRoomDto::class.java)

        roomFiles.forEach { shopConfigFile ->
            try {
                val dto = adapter.fromJson(shopConfigFile.readText())
                if (dto != null) {
                    val shop = DressingRoom(
                        "${shopConfigFile.parentFile.name}/${shopConfigFile.name}",
                        shopConfigFile.parentFile,
                        dto
                    )
                    rooms.add(shop)
                    shop.start()
                }
            } catch (e: Exception) {
                Logger.capture(e)
                logcat(LogPriority.WARN, logToCrew = true) { "DressingRoomDto > Failed to load " + shopConfigFile.path }
            }
        }
    }
}