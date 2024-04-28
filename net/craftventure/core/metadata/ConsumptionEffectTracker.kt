package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.async.executeSync
import net.craftventure.core.metadata.ConsumptionEffectTracker.CancellableEffect
import net.kyori.adventure.text.Component
import okhttp3.internal.toImmutableList
import org.apache.mina.util.ConcurrentHashSet
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.time.Duration

class ConsumptionEffectTracker(
    val player: Player,
) : BasePlayerMetadata(player) {
    private val effects = ConcurrentHashSet<CancellableEffect>()
    private val wearOffs = mutableListOf<WearOff>()

    fun getWearOffs(): List<WearOff> = wearOffs.toImmutableList()

    private fun cleanWearOffs() {
        val now = System.currentTimeMillis()
        wearOffs.removeAll { it.endsAt <= now }
    }

    fun addWearOff(wearOff: WearOff) {
        cleanWearOffs()
        wearOffs.add(wearOff)
    }

    fun getWearOffs(category: String): List<WearOff> {
        cleanWearOffs()
        return wearOffs.filter { it.category == category }
    }

    fun clearAll() {
        effects.forEach { it.cancel() }
        effects.clear()
    }

    fun unregister(effect: CancellableEffect) {
        effects.remove(effect)
    }

    fun register(effect: CancellableEffect) {
        effects.add(effect)
    }

    fun registerTask(task: Int) {
        effects.add(CancellableEffect {
            Bukkit.getScheduler().cancelTask(task)
        })
    }

    fun register(delay: Long, allowCancellation: Boolean = true, action: () -> Unit) {
        var canceller: CancellableEffect? = null
        val task = executeSync(delay) {
            action()
            if (canceller != null)
                unregister(canceller!!)
        }
        if (allowCancellation) {
            canceller = CancellableEffect {
                Bukkit.getScheduler().cancelTask(task)
            }
            effects.add(canceller)
        }
    }

    fun interface CancellableEffect {
        fun cancel()
    }

    override fun debugComponent() = Component.text("effects=${effects.size}")

    data class WearOff(
        val category: String,
        val endsAt: Long,
    ) {
        companion object {
            fun ofDuration(category: String, duration: Duration) =
                WearOff(category, System.currentTimeMillis() + duration.inWholeMilliseconds)
        }
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { ConsumptionEffectTracker(player) }
    }
}