package net.craftventure.core.config

import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.json.MoshiBase.parseFile
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.util.Vector
import java.io.File
import java.io.FileFilter


class AreaConfigManager private constructor() {
    private val areaConfigList = ArrayList<AreaConfig>()

    fun isKartingBlocked(location: Location) = areaConfigList.any {
        it.kartingBlocked && it.area.isInArea(location)
    }

    fun isKartingBlocked(location: Vector) = areaConfigList.any {
        it.kartingBlocked && it.area.isInArea(location)
    }

    fun isConsumptionBlocked(location: Vector) = areaConfigList.any {
        it.itemConsumptionBlocked && it.area.isInArea(location)
    }

    fun isForceVanished(location: Location) = areaConfigList.any {
        it.forceVanish && it.area.isInArea(location)
    }

    fun isForceVanished(location: Vector) = areaConfigList.any {
        it.forceVanish && it.area.isInArea(location)
    }

    fun reload() {
        areaConfigList.clear()
        loadAreas(File(CraftventureCore.getInstance().dataFolder, "data/area"))
        Bukkit.getPluginManager().callEvent(CraftventureAreasReloadedEvent())
    }

    fun loadAreas(directory: File, root: String = "") {
        val files = directory.listFiles(FileFilter { it.isFile || it.isDirectory })
        if (files != null) {
            for (file in files) {
                if (file.isFile) {
                    if (file.name.endsWith(".json") && !file.name.startsWith("_")) {
                        try {
                            val areaConfig = CvMoshi.adapter(AreaConfig::class.java).parseFile(file)!!
                            areaConfig.name = root + file.nameWithoutExtension
                            areaConfigList.add(areaConfig)
                        } catch (e: Exception) {
                            Logger.capture(e)
                            Logger.severe("AreaConfigManager > Failed to load " + file.path, logToCrew = false)
                        }

                    }
                } else if (file.isDirectory && !file.name.startsWith("_")) {
                    loadAreas(file, file.name + "/")
                }
            }
        }
    }

    val enabledAreasConfigList: List<AreaConfig>
        get() = areaConfigList

    fun getAreaConfigList(): List<AreaConfig> {
        return areaConfigList
    }

    companion object {
        private val manager by lazy { AreaConfigManager() }

        @JvmStatic
        fun getInstance() = manager
    }
}
