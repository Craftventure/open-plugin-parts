package net.craftventure.core.feature.balloon.extensions

import com.squareup.moshi.JsonClass
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.utils.spawnParticleX
import org.bukkit.Particle

class DebugExtension : ExtensibleBalloon.Extension() {
    override fun update(balloon: ExtensibleBalloon) {
        balloon.balloonLocation!!.spawnParticleX(Particle.END_ROD)
    }

    @JsonClass(generateAdapter = true)
    class Json : ExtensibleBalloon.Extension.Json() {
        override fun toExtension(): ExtensibleBalloon.Extension = DebugExtension()

        companion object {
            const val type = "debug"
        }
    }
}