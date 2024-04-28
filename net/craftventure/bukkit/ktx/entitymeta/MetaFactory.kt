package net.craftventure.bukkit.ktx.entitymeta

import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.ktx.logging.logcat
import org.bukkit.entity.Player
import java.util.*

object MetaFactory {
    private val services by lazy {
        val loader = ServiceLoader.load(PlayerMetaFactory::class.java, PluginProvider.classLoader)
        loader.stream().toList()
    }
    private val loginServices by lazy { services }

    fun handleLogin(player: Player) {
        logcat { "Applying ${loginServices.size} login stage services" }
        loginServices.forEach {
            val service = it.get()
            try {
                service.create(player)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Failed to apply ${service.javaClass.name}" }
                e.printStackTrace()
            }
        }
    }
}

abstract class PlayerMetaFactory {
    abstract fun create(player: Player): BaseMetadata

    /**
     * Lower = early, higher = late
     */
    open val priority: Int = 1000
}