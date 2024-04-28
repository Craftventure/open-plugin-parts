package net.craftventure.core.extension

import net.craftventure.core.ktx.json.toJson
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.map.renderer.SimpleImageRenderer
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import net.craftventure.database.repository.MapEntriesRepository

fun MapEntriesRepository.createSimpleLoader(name: String, source: String, xFrames: Int, yFrames: Int): Boolean {
    try {
        var imageId = 0
        for (y in 0 until yFrames) {
            for (x in 0 until xFrames) {
                val entry = MapEntry(
                    name + imageId,
                    null,
                    0,
                    "simpleloader",
                    SimpleImageRenderer.SimpleImageData(source, x * -128, y * -128).toJson(),
                )
                create(entry)
                imageId++
            }
        }
        return true
    } catch (e: Exception) {
        Logger.capture(e)
        return false
    }
}