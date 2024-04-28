package net.craftventure.core.feature.nbsplayback

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.nbs.NbsFile
import net.craftventure.core.ktx.util.getRelativeFileName
import java.io.File

object NbsFileManager {
    private var nbsFiles: Map<String, NbsFile> = hashMapOf()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getSongNames() = nbsFiles.keys

    fun reload() {
        scope.launch {
            reloadSuspend()
        }
    }

    private suspend fun reloadSuspend() = coroutineScope {
        val semaphore = Semaphore(1)
        val newFiles = hashMapOf<String, NbsFile>()
        val searchDirectory = File(PluginProvider.getInstance().dataFolder, "data/noteblocksongs")
        val deferreds = mutableListOf<Deferred<*>>()
        searchDirectory.walk()
            .forEach { file ->
                if (!file.isFile) return@forEach
                if (!file.name.equals(file.name.lowercase())) return@forEach

                deferreds += async {
                    try {
                        val nbsFile = NbsFile.readFrom(file)
                        semaphore.withPermit {
                            newFiles[file.getRelativeFileName(withExtension = false, root = searchDirectory)] = nbsFile
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logcat(
                            logToCrew = true,
                            priority = LogPriority.WARN
                        ) { "Failed to read NBS file ${file.name}: ${e.message}" }
                    }
                }
            }
        deferreds.awaitAll()
        nbsFiles = newFiles
        logcat(logToCrew = true) { "Reloaded all ${nbsFiles.size} NBS songs" }
    }

    fun getSong(id: String) = nbsFiles[id]
}