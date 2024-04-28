package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BaseMetadata
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.ktx.logging.logcat
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class TestMeta(
    val player: Player,
) : BasePlayerMetadata(player) {
    init {
        logcat { "Created test meta for ${player.name}" }
    }

    override fun onDestroy() {
        super.onDestroy()
        logcat { "Destroying test meta" }
    }

    override fun debugComponent() = Component.text("player=${player.name}")

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { TestMeta(player) }
    }
}