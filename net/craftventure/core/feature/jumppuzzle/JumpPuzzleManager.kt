package net.craftventure.core.feature.jumppuzzle

import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import org.bukkit.entity.Player
import java.io.File


object JumpPuzzleManager {
    private val puzzles = mutableListOf<JumpPuzzle>()

    fun getPuzzles(): List<JumpPuzzle> = puzzles

    fun reload() {
        puzzles.forEach { it.stop() }
        puzzles.clear()

        val directory = File(CraftventureCore.getInstance().dataFolder, "data/parkour")
        directory.listFiles()?.forEach {
            try {
                val config = CvMoshi.adapter(JumpPuzzle.Json::class.java).fromJson(it.readText())!!
                puzzles.add(config.create())
            } catch (e: Exception) {
                Logger.warn("Failed to load parkour ${it.name}", logToCrew = true)
                Logger.capture(e)
                return@forEach
            }
        }

        for (puzzle in puzzles) {
            puzzle.start()
        }
    }

    fun init() {
        reload()
    }

    fun getParticipatingJumpPuzzle(player: Player) = puzzles.firstOrNull { it.isPlaying(player) }
    fun getParticipatingJumpData(player: Player) = puzzles.firstOrNull { it.isPlaying(player) }?.getData(player)

    fun destroy() {
        for (puzzle in puzzles) {
            puzzle.stop()
        }
        puzzles.clear()
    }
}