package net.craftventure.core.task

import net.craftventure.backup.service.BackupService
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import org.bukkit.Bukkit

object BackupTask {
    private var lastBackup: Long = System.currentTimeMillis() - (20 * 60 * 1000)
    private var intialised = false

    fun init() {
        if (intialised)
            return
        BackupService.init(CraftventureCore.getInstance(), MainRepositoryProvider.allRepositories)
        if (PluginProvider.isNonProductionServer()) {
            return
        }

        intialised = true

        Logger.info("Backup scheduler active for this server instance")
        Bukkit.getServer().getPluginCommand("backup")!!.setExecutor(CommandBackup())
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            if (BackupService.isRunning())
                return@scheduleSyncRepeatingTask

            if (lastBackup < System.currentTimeMillis() - (60 * 60 * 1000)) {
                lastBackup = System.currentTimeMillis()
                Logger.info("Starting scheduled backup")
                BackupService.start(CraftventureCore.getInstance())
            }
        }, 20 * 60, 20 * 60)
    }
}