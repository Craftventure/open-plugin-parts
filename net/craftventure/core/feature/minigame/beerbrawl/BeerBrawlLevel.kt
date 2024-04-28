package net.craftventure.core.feature.minigame.beerbrawl

import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.feature.minigame.BaseMinigameLevel
import org.bukkit.Location


class BeerBrawlLevel(
    id: String,
    maxPlayers: Int = 5,
    playTimeInSeconds: Int = if (PluginProvider.isTestServer()) 5 else 24 + (60 * 3) + 10,
    var startLocation: Location,
    val beerLocation: Location,
    area: Area,
    val toiletArea: SimpleArea = SimpleArea("world", )
) : BaseMinigameLevel(
    id,
    maxPlayers,
    playTimeInSeconds,
    area
) {
    override fun toJson(): Json {
        TODO("Not yet implemented")
    }
}
