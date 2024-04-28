package net.craftventure.bukkit.ktx.util

import org.bukkit.Particle
import java.util.*

object ParticleUtils {
    fun particleByName(name: String): Particle? {
        val particle = when (name.lowercase(Locale.getDefault())) {
            "splash" -> Particle.WATER_SPLASH
            "magiccrit" -> Particle.CRIT_MAGIC
            "largesmoke" -> Particle.SMOKE_LARGE
            "endrod" -> Particle.END_ROD
            "instantspell" -> Particle.SPELL_INSTANT
            "driplava" -> Particle.DRIP_LAVA
            "dripwater" -> Particle.DRIP_WATER
            "enchantmenttable" -> Particle.ENCHANTMENT_TABLE
            "dragonbreath" -> Particle.DRAGON_BREATH
            "witchmagic" -> Particle.SPELL_WITCH
            "hugeexplosion" -> Particle.EXPLOSION_HUGE
            "explode" -> Particle.EXPLOSION_NORMAL
            "fireworksspark" -> Particle.FIREWORKS_SPARK
            "droplet" -> Particle.WATER_DROP
            "blockcrack" -> Particle.BLOCK_CRACK
            "smoke" -> Particle.SMOKE_NORMAL
            "largeSmoke" -> Particle.SMOKE_LARGE
            "bubble" -> Particle.WATER_BUBBLE
            "largeexplode" -> Particle.EXPLOSION_LARGE
            else -> null
        }
        if (particle != null) return particle
        try {
            return Particle.valueOf(name.toUpperCase())
        } catch (e: Exception) {
        }
        return null
    }
}