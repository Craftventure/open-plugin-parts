package net.craftventure.core.script.particle

import com.google.gson.annotations.Expose
import net.craftventure.bukkit.ktx.util.ParticleUtils.particleByName
import net.craftventure.core.ktx.util.Logger.warn
import org.bukkit.Particle
import java.util.*

class ParticlePath {
    @Expose
    val particle: String? = null

    @Expose
    val longDistance = false

    @Expose
    val updateTick: Long? = null

    @Expose
    val targetDuration: Long = 0

    @Expose
    val repeat = true

    @Expose
    val nodes: List<ParticleValue>? = LinkedList()
    private var groupId: String? = null
    private var name: String? = null
    private var cachedParticle: Particle? = null

    //                e.printStackTrace();
    val particleType: Particle?
        get() {
            if (cachedParticle == null) {
                try {
                    cachedParticle = particleByName(particle!!)
                } catch (e: Exception) { //                e.printStackTrace();
                }
                if (cachedParticle == null) {
                    warn(
                        "Particle %s not found, using fireworksSpark instead for groupId=%s name=%s",
                        true,
                        particle,
                        groupId,
                        name
                    )
                    cachedParticle = Particle.FIREWORKS_SPARK
                }
            }
            return cachedParticle
        }

    fun isValid(groupId: String?, name: String?): Boolean {
        this.groupId = groupId
        this.name = name
        return nodes != null && nodes.size > 0 && particleType != null
    }

}