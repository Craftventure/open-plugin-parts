package net.craftventure.core.feature.minigame

import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import net.craftventure.database.type.BankAccountType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration

interface Minigame {
    val isRunning: Boolean
    val displayName: String
    val internalName: String
    val minRequiredPlayers: Int
    val maxPlayers: Int
    var isFromConfig: Boolean

    /**
     * in millis
     */
    val levelBaseTimeLimit: Long

    /**
     * In millis
     */
    val timeLeft: Long?

    /**
     * Current playtime in millis
     */
    val playTime: Long

    fun isPlaying(player: Player): Boolean
    fun join(player: Player, announce: Boolean): Boolean
    fun leave(player: Player, reason: LeaveReason): Boolean
    fun isOnStandby(player: Player): Boolean

    fun canJoin(player: Player): Boolean
    fun canStart(): Boolean
    fun start(players: List<Player>): Boolean
    fun stop(reason: StopReason): Boolean

    fun represent(): ItemStack = ItemStack(Material.TNT)
    fun provideWarp(): Warp? = null
    fun describeGameplay(): String
    fun describeBalanceRewards(): List<BalanceReward> = emptyList()
    fun provideDuration(): MinigameDuration

    fun destroy()

    enum class StopReason {
        FAILURE,
        ALL_PLAYERS_FINISHED,
        OUT_OF_TIME,
        TOO_FEW_PLAYERS,
        SERVER_STOP
    }

    enum class LeaveReason {
        LEAVE,
        DISQUALIFIED,
        GAME_STOPPING
    }

    /**
     * TODO: Remove state, in the future every game should manage their own state
     * and just have the isRunning val
     */
    enum class State {
        IDLE,
        PREPARING_GAME,
        RUNNING,
        STOPPING_GAME
    }

    data class BalanceReward(val accountType: BankAccountType, val reward: Int, val description: String)

    enum class SubType constructor(val description: String?) {
        MINIGAME("Minigame"),
        DUNGEON("Dungeon")
    }

    class MinigameDuration(
        val duration: Duration,
        val durationType: DurationType
    )

    enum class DurationType {
        EXACT,
        MINIMUM,
        MAXIMUM,
    }

    abstract class Json {
        var saveScores: Boolean = true
        var warpName: String? = null
        var representationItem: String? = null
        lateinit var description: String
        lateinit var lobby: Lobby.Json
        var lobbySigns: Set<Location>? = null
        lateinit var internalName: String
        lateinit var displayName: String
        var minRequiredPlayers: Int = 3
        lateinit var exitLocation: Location

        abstract fun createGame(): Minigame

        open fun createLobby(minigame: Minigame): Lobby = lobby.create(minigame)

        fun createLobbyListeners(lobby: Lobby) = lobbySigns?.map { LobbySignListener(lobby, it) } ?: emptyList()
    }
}