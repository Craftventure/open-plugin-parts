package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.plugin.Environment
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

object BackupRestorer {
    fun getDefaultStoreFile() = File(CraftventureCore.getInstance().dataFolder, "world.zip")

    @JvmOverloads
    @JvmStatic
    fun restoreWorldIfNeeded(zipFile: File = getDefaultStoreFile()): Boolean {
        if (PluginProvider.environment == Environment.PRODUCTION) return false
        if (zipFile.exists())
            return restoreWorld(zipFile)
        return false
    }

    fun restoreWorld(zipFile: File): Boolean {
        val worldFolder = File("world")//File(Bukkit.getServer().worldContainer.absoluteFile, "world")
        Logger.info("Restoring world ${zipFile.path}")
        if (worldFolder.exists()) {
            Logger.info("Deleting $worldFolder (${Bukkit.getServer().worldContainer.absoluteFile})...")
            if (!worldFolder.deleteRecursively()) throw IOException("Failed to delete world")
        }

        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            Logger.info("Copying world...")
            var entry = zip.nextEntry
            val buffer = ByteArray(1024)
            while (entry != null) {
                val file = File(worldFolder, entry.name)
//                Logger.debug("Writing ${entry.name} ${entry.isDirectory} to $file")
                if (entry.isDirectory) {
                    if (!file.exists() && !file.mkdirs())
                        throw IOException("Failed to create zip directory $file")
                } else {
                    val directory = File(file.parent)
                    if (!directory.exists() && !directory.mkdirs())
                        throw IOException("Failed to create file directory $directory")

                    if (!file.exists() && !file.createNewFile())
                        throw IOException("Failed to create file to extract $file")

                    file.outputStream().use { fileOutputStream ->
                        var len: Int
                        while (zip.read(buffer).also { len = it } > 0) {
                            fileOutputStream.write(buffer, 0, len)
                        }
                    }
                }
                entry = zip.nextEntry
            }
            Logger.debug("Copying world done")
        }
        if (!zipFile.delete())
            throw IOException("Failed to delete ZIP file after restore")
        return true
    }
}