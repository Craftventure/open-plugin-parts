package net.craftventure.bukkit.ktx.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.Particle.DustTransition
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack

class ParticleAdapter {
    @FromJson
    fun fromJson(json: String) = Particle.values().filter { it.name.equals(json, ignoreCase = true) }

    @ToJson
    fun toJson(instance: Particle) = instance.name


    sealed class ParticleOptionJson {
        abstract fun create(): Any
    }

    @JsonClass(generateAdapter = true)
    class DustOptionsJson(
        val color: Color,
        val size: Float,
    ) : ParticleOptionJson() {
        override fun create() = DustOptions(color, size)
    }

    @JsonClass(generateAdapter = true)
    class DustTransitionJson(
        val from: Color,
        val to: Color,
        val size: Float,
    ) : ParticleOptionJson() {
        override fun create() = DustTransition(from, to, size)
    }

    @JsonClass(generateAdapter = true)
    class ItemStackJson(
        val data: String,
    ) : ParticleOptionJson() {
        override fun create(): ItemStack {
            val itemStack = ItemStack(Material.AIR)
            return itemStack
        }
    }

    @JsonClass(generateAdapter = true)
    class BlockDataJson(
        val data: String,
    ) : ParticleOptionJson() {
        override fun create(): BlockData {
            return Bukkit.getServer().createBlockData(data)
        }
    }

    @JsonClass(generateAdapter = true)
    class FloatJson(
        val data: Float,
    ) : ParticleOptionJson() {
        override fun create(): Float = data
    }

    @JsonClass(generateAdapter = true)
    class IntegerJson(
        val data: Int,
    ) : ParticleOptionJson() {
        override fun create(): Int = data
    }
}