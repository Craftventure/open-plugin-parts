package net.craftventure.core.feature.balloon.extensions

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Particle

class PopExtension(private val data: Json) : ExtensibleBalloon.Extension() {
    override fun despawn(balloon: ExtensibleBalloon, withEffects: Boolean, tracker: NpcEntityTracker) {
        val location = balloon.balloonLocation!!
        if (withEffects) {
            val players = tracker.players
            location.spawnParticleX(
                data.particle,
                data.count,
                data.offsetX,
                data.offsetY,
                data.offsetZ,
                data.extra,
                players = players,
            )
            if (data.sound != null)
                players.forEach { it.playSound(location, data.sound, data.volume, data.pitch) }
        }
    }

    @JsonClass(generateAdapter = true)
    class Json(
        val sound: String? = SoundUtils.BALLOON_POP,
        val volume: Float = 2f,
        val pitch: Float = 1f,
        val particle: Particle = Particle.EXPLOSION_NORMAL,
        val count: Int = 20,
        val offsetX: Double = 0.2,
        val offsetY: Double = 0.2,
        val offsetZ: Double = 0.2,
        val extra: Double = 0.1,
    ) : ExtensibleBalloon.Extension.Json() {
        override fun toExtension(): ExtensibleBalloon.Extension = PopExtension(this)

        companion object {
            const val type = "pop"
        }
    }
}