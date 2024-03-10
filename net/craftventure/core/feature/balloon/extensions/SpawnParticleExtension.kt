package net.craftventure.core.feature.balloon.extensions

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.json.ParticleAdapter
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.utils.ParticleSpawner
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Particle
import org.bukkit.util.Vector

class SpawnParticleExtension(
    private val data: Json,
) : ExtensibleBalloon.Extension() {
    private var currentTick = 0
    override fun update(balloon: ExtensibleBalloon) {
        super.update(balloon)

        val location = balloon.balloonLocation!!.clone()
        if (data.locationOffset != null)
            location.add(data.locationOffset)
        data.tick?.let { tick ->
            currentTick++
            if (currentTick < tick) {
                return
            }
            currentTick = 0
        }

        this.data.apply {
            val parsedData = this.data?.create()
            location.spawnParticleX(
                particle = particle,
                count = count,
                offsetX = offsetX,
                offsetY = offsetY,
                offsetZ = offsetZ,
                extra = extra,
                longDistance = longDistance,
                range = range,
                data = parsedData,
            )
        }
    }

    @JsonClass(generateAdapter = true)
    class Json(
        val tick: Int? = null,
        val particle: Particle,
        val count: Int = 1,
        val offsetX: Double = 0.0,
        val offsetY: Double = 0.0,
        val offsetZ: Double = 0.0,
        val extra: Double = 0.0,
        val longDistance: Boolean = false,
        val range: Double = ParticleSpawner.DEFAULT_RANGE,
        val locationOffset: Vector? = null,
        val data: ParticleAdapter.ParticleOptionJson? = null,
    ) : ExtensibleBalloon.Extension.Json() {
        override fun toExtension(): ExtensibleBalloon.Extension = SpawnParticleExtension(this)

        companion object {
            const val type = "spawn_particle"
        }
    }
}