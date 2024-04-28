package net.craftventure.core.manager

import net.craftventure.core.CraftventureCore
import net.craftventure.core.animation.ModeledDoor
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.logging.logcat
import java.io.File

object DoorManager {
    private val doors: MutableMap<String, ModeledDoor> = mutableMapOf()

    val keys get() = doors.keys

    fun get(id: String) = doors[id]

    @JvmOverloads
    fun reload(force: Boolean = false) {
        val existingDoors = LinkedHashMap(doors)
        doors.clear()

        val directory = File(CraftventureCore.getInstance().dataFolder, "data/door")
        directory.listFiles()?.filter { !it.isDirectory }?.forEach { file ->
            try {
                val adapter = CvMoshi.adapter(ModeledDoor.ModeledDoorConfig::class.java)
                val config = adapter.fromJson(file.readText())!!

                val existingDoor = existingDoors[file.nameWithoutExtension]
                if (existingDoor != null && existingDoor.config == config) {
                    // Reuse existing door as config didn't change
                    doors[file.nameWithoutExtension] = existingDoor
                    existingDoors.remove(file.nameWithoutExtension)
                    return@forEach
                }

                val door = ModeledDoor(config)
                doors[file.nameWithoutExtension] = door
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Failed to load door ${file}" }
                e.printStackTrace()
            }
        }

        existingDoors.forEach {
            it.value.destroy()
        }
    }
}