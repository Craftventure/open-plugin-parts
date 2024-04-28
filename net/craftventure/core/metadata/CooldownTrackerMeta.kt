package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class CooldownTrackerMeta(
    val player: Player,
) : BasePlayerMetadata(player) {
    private val data = ConcurrentHashMap<String, Long>()

    fun use(category: String, timeout: Long): Boolean {
        val lowercaseCategory = category.lowercase()
        val now = System.currentTimeMillis()
        val lastUse = data[lowercaseCategory] ?: 0

        val canUse = now - timeout > lastUse
        if (canUse) {
            data[lowercaseCategory] = now
        }
        return canUse
    }

    override fun debugComponent() = Component.text("data=${data.map { "${it.value}/${it.key}" }}")

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { CooldownTrackerMeta(player) }
    }
}