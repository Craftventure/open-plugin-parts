package net.craftventure.core.script.particle

import com.google.gson.annotations.Expose
import java.util.*

class ParticleMap {
    @Expose
    val paths: List<ParticlePath> = LinkedList()
}