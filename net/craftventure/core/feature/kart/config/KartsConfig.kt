package net.craftventure.core.feature.kart.config

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.kart.KartPart

@JsonClass(generateAdapter = true)
data class KartsConfig(
    val kart: List<KartOptions> = emptyList(),
    val brake: List<KartBrakes> = emptyList(),
    val tire: List<KartTires> = emptyList(),
    val engine: List<KartEngine> = emptyList(),
    val handling: List<KartHandling> = emptyList(),
    val steer: List<KartSteer> = emptyList(),
    val zeppelinLifters: List<KartZeppelinLifter> = emptyList(),
    val planeLifters: List<KartPlaneLifter> = emptyList(),
) {
    val isValid: Boolean
        get() = kart.distinctBy { it.id }.size == kart.size &&
                brake.distinctBy { it.id }.size == brake.size &&
                tire.distinctBy { it.id }.size == tire.size &&
                engine.distinctBy { it.id }.size == engine.size &&
                handling.distinctBy { it.id }.size == handling.size &&
                steer.distinctBy { it.id }.size == steer.size &&
                zeppelinLifters.distinctBy { it.id }.size == zeppelinLifters.size &&
                planeLifters.distinctBy { it.id }.size == planeLifters.size

    private fun requireUniqueIds(
        existing: List<KartPart>,
        newItems: List<KartPart>,
        errorGenerator: (String) -> String
    ) {
        val ids = existing.map { it.id }

        newItems.forEach {
            if (it.id in ids)
                throw IllegalStateException(errorGenerator(it.id))
        }
    }

    fun mergeWith(overlay: KartsConfig): KartsConfig {
        requireUniqueIds(kart, overlay.kart) { "Kart with ID $it defined more than once" }
        requireUniqueIds(brake, overlay.brake) { "Brake with ID $it defined more than once" }
        requireUniqueIds(tire, overlay.tire) { "Tire with ID $it defined more than once" }
        requireUniqueIds(engine, overlay.engine) { "Engine with ID $it defined more than once" }
        requireUniqueIds(handling, overlay.handling) { "Handling with ID $it defined more than once" }
        requireUniqueIds(steer, overlay.steer) { "Steer with ID $it defined more than once" }
        requireUniqueIds(
            zeppelinLifters,
            overlay.zeppelinLifters
        ) { "ZeppelinLifter with ID $it defined more than once" }
        requireUniqueIds(
            planeLifters,
            overlay.planeLifters
        ) { "ZeppelinLifter with ID $it defined more than once" }

        return KartsConfig(
            kart + overlay.kart,
            brake + overlay.brake,
            tire + overlay.tire,
            engine + overlay.engine,
            handling + overlay.handling,
            steer + overlay.steer,
            zeppelinLifters + overlay.zeppelinLifters,
            planeLifters + overlay.planeLifters,
        )
    }
}