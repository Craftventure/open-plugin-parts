package net.craftventure.audioserver

import net.craftventure.core.api.CvApi
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.util.Logger
import okhttp3.Request

object AudioServerAreaTester {
    @JvmStatic
    fun test() {
        executeAsync {
            val areas = AudioServer.instance.audioServerConfig!!.areas.toList()
            for (area in areas) {
                for (resource in area.resources) {
                    try {
                        val audio =
                            CvApi.okhttpClient.newCall(Request.Builder().url(resource.location).build()).execute()
                        if (!audio.isSuccessful) {
                            Logger.severe("Failed to load AudioServer resource ${resource.location} for area ${area.name}")
                        }
                        audio.close()
                    } catch (e: Exception) {
                        Logger.severe("Failed to load AudioServer resource ${resource.location} for area ${area.name}: ${e.message}")
                    }
                }
            }
            Logger.info("Checked all AudioServer resources!")
        }
    }
}