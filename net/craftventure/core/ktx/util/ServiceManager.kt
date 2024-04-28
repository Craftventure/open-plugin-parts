package net.craftventure.core.ktx.util

import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.service.EnableService
import net.craftventure.core.ktx.service.LoadService
import java.util.*

object ServiceManager {
    private inline fun <reified T : Any> run(classLoader: ClassLoader, runner: (T) -> Unit) {
        val loader = ServiceLoader.load(T::class.java, classLoader)
        val items = loader.stream().toList()
//        logcat { "Applying ${items.size} ${T::class.simpleName} services" }
        items.forEach {
            val service = it.get()
            try {
                runner(service)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Failed to apply ${service::class.java.name}" }
                e.printStackTrace()
            }
        }
    }

    fun doLoadStage(classLoader: ClassLoader) {
        run<LoadService>(classLoader) { it.init() }
    }

    fun doEnableStage(classLoader: ClassLoader) {
        run<EnableService>(classLoader) { it.init() }
    }
}