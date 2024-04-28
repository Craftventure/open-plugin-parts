package net.craftventure.bukkit.ktx.plugin

import net.craftventure.core.ktx.util.ServiceManager
import org.bukkit.plugin.java.JavaPlugin

object PluginProvider {
    lateinit var plugin: CvPlugin
        private set

    @JvmStatic
    val classLoader
        get() = plugin.pluginClassLoader

    @JvmStatic
    var environment: Environment
        get() = plugin.environment
        set(value) {
            plugin.environment = value
        }

    @JvmStatic
    fun getInstance() = plugin

    @JvmStatic
    fun getMainThread() = getInstance().mainThread

    @JvmStatic
    fun isOnMainThread() = getInstance().isOnMainThread()

    @JvmStatic
    fun isTestServer(): Boolean {
        return environment == Environment.DEVELOPMENT
    }

    @JvmStatic
    fun isNonProductionServer(): Boolean {
        return environment != Environment.PRODUCTION
    }

    @JvmStatic
    fun isProductionServer(): Boolean {
        return environment == Environment.PRODUCTION
    }

    abstract class CvPlugin : JavaPlugin() {
        lateinit var mainThread: Thread
        lateinit var environment: Environment


        val pluginClassLoader get() = classLoader

        override fun onLoad() {
            super.onLoad()
            mainThread = Thread.currentThread()
            plugin = this

            ServiceManager.doLoadStage(classLoader)
        }

        fun isOnMainThread(): Boolean {
            return Thread.currentThread() === mainThread
        }

        open fun isTestServer(): Boolean {
            return environment == Environment.DEVELOPMENT
        }

        open fun isNonProductionServer(): Boolean {
            return environment != Environment.PRODUCTION
        }

        open fun isProductionServer(): Boolean {
            return environment == Environment.PRODUCTION
        }
    }
}