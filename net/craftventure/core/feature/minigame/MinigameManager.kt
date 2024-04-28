package net.craftventure.core.feature.minigame

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.setColor
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.markAsWornItem
import net.craftventure.core.feature.minigame.beerbrawl.BeerBrawl
import net.craftventure.core.feature.minigame.beerbrawl.BeerBrawlLevel
import net.craftventure.core.feature.minigame.lasergame.*
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


object MinigameManager {
    private val minigames = mutableSetOf<Minigame>()
    private val lobbies = mutableSetOf<Lobby>()

    fun init() {
        val world = Bukkit.getWorld("world")!!
//        if (CraftventureCore.isTestServer())
//        addAutopia(world)
        addBeerGame(world)
//        if (PluginProvider.isNonProductionServer())
        setupLaserGameArenas(world)
        addLasergameFfaLobby(world)
//        addLasergameTeamsLobby(world)
        reload()
    }

    fun reload() {
        val configGames = minigames.filter { it.isFromConfig }
        configGames.forEach { game ->
            game.destroy()
            val lobbiesToDelete = lobbies.filter { it.minigame === game }
            for (lobby in lobbiesToDelete) {
                LobbySignListener.cleanup(lobby)
                lobby.destroy()
            }
            lobbies.removeAll(lobbiesToDelete)
        }
        minigames.removeAll(configGames)

        val directory = File(CraftventureCore.getInstance().dataFolder, "data/minigame")
        directory.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { gameConfigFile ->
            try {
                val gameConfig = loadGame(gameConfigFile)
                try {
                    val game = gameConfig.createGame()
                    game.isFromConfig = true

                    val lobby = gameConfig.createLobby(game)
                    lobbies.add(lobby)

                    gameConfig.createLobbyListeners(lobby).forEach {
                        it.register()
                    }

                    minigames.add(game)
                } catch (e: Exception) {
                    e.printStackTrace()
//                        Logger.capture(e)
                    Logger.severe(
                        "Failed to properly initialize minigame from config ${gameConfigFile.path} (${e.message}), it's advised to restart the server as it may be corrupted in it's current state. See console for full message",
                        logToCrew = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
//                    Logger.capture(e)
                Logger.severe(
                    "Failed to load minigame ${gameConfigFile.path}: ${e.message}. See console for full message",
                    logToCrew = true
                )
            }
        }
    }

    private fun loadGame(file: File): Minigame.Json {
        val configContent = file.readText()
        val configAdapter = CvMoshi.adapter(Minigame.Json::class.java)
        return configAdapter.fromJson(configContent)!!
    }

    fun destroy() {
        for (minigame in minigames) {
            minigame.destroy()
            for (lobby in lobbies) {
                LobbySignListener.cleanup(lobby)
                lobby.destroy()
            }
            lobbies.clear()
        }
    }

    fun all() = minigames.toList()

    fun byId(id: String): Minigame? = minigames.firstOrNull { it.internalName == id }
    fun lobbyById(id: String): Lobby? = lobbies.firstOrNull { it.id == id }

    fun leaveLobby(player: Player): Boolean {
        lobbies.forEach {
            if (it.tryLeave(player)) {
                return true
            }
        }
        return false
    }

    fun getParticipatingGame(player: Player): Minigame? =
        minigames.firstOrNull { it.isPlaying(player) } ?: lobbies.firstOrNull { it.isQueued(player) }?.minigame

    private fun generateSignLobby(
        game: Minigame, location: Location, lobbyCreator: () -> Lobby = {
            BaseLobby(
                minigame = game,
                id = game.internalName
            )
        }
    ) {
        val lobby = lobbyCreator()
        lobbies.add(lobby)
        val sign = LobbySignListener(lobby, location)
        sign.register()
    }

    private fun setupLaserGameArenas(world: World) {
        addLaserGameArenaFrostPunk(world)
        addLaserGameArenaCake(world)
        addLaserGameArenaEndor(world)
        addLaserGameArenaMagmaCavern(world)
    }

    private fun addLaserGameArenaCake(world: World) {
        val area = SimpleArea("world", )
        val arena = LaserGameArena(
            arenaId = "cake",
            area = area,
            arenaMode = LaserGameArenaMode.DEATHMATCH,
            teamMode = LaserGameTeamMode.FFA,
            spawns = arrayOf(
                Location(Bukkit.getWorld("world"), 59f),
                Location(Bukkit.getWorld("world"), 7f),
                Location(Bukkit.getWorld("world"), 36f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), 6f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), 4f),
                Location(Bukkit.getWorld("world"), 87f),
                Location(Bukkit.getWorld("world"), ),
                Location(Bukkit.getWorld("world"), 5f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), 5f)
            )
        )
        LaserGameArenaPool.register(arena)
    }

    private fun addLaserGameArenaEndor(world: World) {
        val area = SimpleArea("world", )
        val arena = LaserGameArena(
            arenaId = "endor",
            area = area,
            arenaMode = LaserGameArenaMode.DEATHMATCH,
            teamMode = LaserGameTeamMode.FFA,
            spawns = arrayOf(
                Location(Bukkit.getWorld("world"), 21f),
                Location(Bukkit.getWorld("world"), 82f),
                Location(Bukkit.getWorld("world"), 26f),
                Location(Bukkit.getWorld("world"), 9f),
                Location(Bukkit.getWorld("world"), 1f),
                Location(Bukkit.getWorld("world"), 26f),
                Location(Bukkit.getWorld("world"), 23f),
                Location(Bukkit.getWorld("world"), 3f),
                Location(Bukkit.getWorld("world"), 9f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), 26f)
            )
        )
        LaserGameArenaPool.register(arena)
    }

    private fun addLaserGameArenaMagmaCavern(world: World) {
        val area = SimpleArea("world",)
        val arena = LaserGameArena(
            arenaId = "magma_cavern",
            area = area,
            arenaMode = LaserGameArenaMode.DEATHMATCH,
            teamMode = LaserGameTeamMode.FFA,
            spawns = arrayOf(
                Location(Bukkit.getWorld("world"), ),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), f),
                Location(Bukkit.getWorld("world"), 8f),
                Location(Bukkit.getWorld("world"), 0f),
                Location(Bukkit.getWorld("world"), 23f),
                Location(Bukkit.getWorld("world"), 59f),
                Location(Bukkit.getWorld("world"), ),
                Location(Bukkit.getWorld("world"), f)
            )
        )
        LaserGameArenaPool.register(arena)
    }

    private fun addLaserGameArenaFrostPunk(world: World) {
        val area = SimpleArea("world", )
        val arena = LaserGameArena(
            arenaId = "frostpunk",
            area = area,
            arenaMode = LaserGameArenaMode.DEATHMATCH,
            teamMode = LaserGameTeamMode.FFA,
            spawns = arrayOf(
                Location(Bukkit.getWorld("world"), ),
                Location(Bukkit.getWorld("world"), .85f),
                Location(Bukkit.getWorld("world"), 5f),
                Location(Bukkit.getWorld("world"), 50f),
                Location(Bukkit.getWorld("world"), .15f),
                Location(Bukkit.getWorld("world"), .30f),
                Location(Bukkit.getWorld("world"), 03f)
            )
        )
        LaserGameArenaPool.register(arena)
    }

    private fun addLasergameFfaLobby(world: World) {
        val ffaGame = LaserGame(
            id = "lasergame",
            minRequiredPlayers = if (PluginProvider.isTestServer()) 1 else if (PluginProvider.isNonProductionServer()) 2 else 3,
            minKeepPlayingRequiredPlayers = if (PluginProvider.isTestServer()) 1 else 2,
            name = "LaserGame/FFA",
            exitLocation = Location(Bukkit.getWorld("world"), ),
            teamMode = LaserGameTeamMode.FFA,
            arenaMode = LaserGameArenaMode.DEATHMATCH,
            maxPlayers = if (PluginProvider.isTestServer()) 2 else 12,
            levelBaseTimeLimit = TimeUnit.SECONDS.toMillis(60 * 3),
            description = "A free for all fight with laserguns and turrets. Make the most kills to win.",
            representationItem = MaterialConfig.dataItem(Material.FIREWORK_STAR, 2)
                .displayName(CVTextColor.serverNoticeAccent + "Default gun")
                .setColor(
                    Color.fromRGB(
                        CVTextColor.serverNotice.red(),
                        CVTextColor.serverNotice.blue(),
                        CVTextColor.serverNotice.green()
                    )
                ).markAsWornItem(),
            warpName = "lasergame",
        )
        val lobby = BaseLobby(
            minigame = ffaGame,
            id = ffaGame.internalName,
//            nonVipAfter = Date(Long.MAX_VALUE)
        )
        lobbies.add(lobby)
        val sign = LobbySignListener(lobby, Location(Bukkit.getWorld("world"), ))
        sign.register()
//        generateSignLobby(ffaGame, Location(Bukkit.getWorld("world"), 162.0, 39.0, -738.0))
    }

    private fun addLasergameTeamsLobby(world: World) {
        val ffaGame = LaserGame(
            id = "lasergame",
            minRequiredPlayers = if (PluginProvider.isTestServer()) 1 else 3,
            minKeepPlayingRequiredPlayers = 1,
            name = "LaserGame/Teams",
            exitLocation = Location(Bukkit.getWorld("world"), ),
            teamMode = LaserGameTeamMode.TEAMS,
            arenaMode = LaserGameArenaMode.DEATHMATCH,
            maxPlayers = if (PluginProvider.isTestServer()) 2 else 12,
            levelBaseTimeLimit = TimeUnit.SECONDS.toMillis(60 * 3),
            description = "A team fight with laserguns and turrets. Make the most kills to win.",
            representationItem = MaterialConfig.dataItem(Material.FIREWORK_STAR, 2)
                .displayName(CVTextColor.serverNoticeAccent + "Default gun")
                .setColor(
                    Color.fromRGB(
                        CVTextColor.serverNotice.red(),
                        CVTextColor.serverNotice.blue(),
                        CVTextColor.serverNotice.green()
                    )
                ).markAsWornItem(),
            warpName = "lasergame",
        )
        val lobby = BaseLobby(
            minigame = ffaGame,
            id = ffaGame.internalName,
            nonVipAfter = Date(Long.MAX_VALUE)
        )
        lobbies.add(lobby)
        val sign = LobbySignListener(lobby, Location(Bukkit.getWorld("world"), ))
        sign.register()
//        generateSignLobby(ffaGame, Location(Bukkit.getWorld("world"), 162.0, 39.0, -738.0))
    }

    private fun addBeerGame(world: World) {
        val level = BeerBrawlLevel(
            "beerfind_tavern2",
            startLocation = Location(world, ),
            area = SimpleArea("world",),
            beerLocation = Location(world, )
        )
        val game = BeerBrawl(
            id = "beerbrawl_tavern2",
            minigameLevel = level,
            name = "Beer Brawl",
            exitLocation = Location(world,),
            description = "Help the Father by serving him the correct beer in the tapping room. Return the most beers for the duration of the game to win.",
            representationItem = MaterialConfig.dataItem(Material.DIAMOND_HOE, 83),
            warpName = "beerbrawl"
        )
        minigames.add(game)
        generateSignLobby(game, Location(world, ))
    }
}