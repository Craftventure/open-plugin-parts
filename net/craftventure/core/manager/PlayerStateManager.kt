package net.craftventure.core.manager

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.feature.dressingroom.DressingRoom
import net.craftventure.core.feature.finalevent.FinaleCinematic
import net.craftventure.core.feature.jumppuzzle.JumpPuzzle
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.minigame.BaseLobby
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigameManager
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ride.RideInstance
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.queue.RideQueue
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object PlayerStateManager {
    fun getOrCreate(player: Player) = player.getOrCreateMetadata { PlayerState(player) }

    fun Player.isOperatingRide(playerState: PlayerState? = getMetadata()) = playerState?.operatingRide != null
    fun Player.isInRide(playerState: PlayerState? = getMetadata()) = playerState?.ride != null
    fun Player.isInRideQueue(playerState: PlayerState? = getMetadata()) = playerState?.rideQueue != null

    fun Player.isInDressingRoom(playerState: PlayerState? = getMetadata()) = playerState?.dressingRoom != null

    fun Player.isInMiniGame(playerState: PlayerState? = getMetadata()) = playerState?.minigame != null
    fun Player.isInMiniGameLobby(playerState: PlayerState? = getMetadata()) = playerState?.minigameLobby != null

    fun Player.isInKart(playerState: PlayerState? = getMetadata()) = playerState?.kart != null
    fun Player.isInJumpPuzzle(playerState: PlayerState? = getMetadata()) = playerState?.jumpPuzzle != null

    fun Player.gameState() = getMetadata<PlayerState>()

    private val denyWhileInDressingRoom = Deny("Not allowed while in a dressing room")
    private val denyWhileInKart = Deny("Not allowed while driving a kart")
    private val denyWhileFinale = Deny("Teleporting is currently disabled")
    private val denyWhileInMinigame = Deny("Not allowed while playing a minigame")
    private val denyWhileInMinigameLobby = Deny("Not allowed while lobbying for a minigame")
    private val denyWhileInRide = Deny("Not allowed while riding a ride")
    private val denyWhileInRideQueue = Deny("Not allowed while queueing for a ride")
    private val denyWhileInAnyVehicle = Deny("Not allowed while riding any vehicle")
    private val denyNoMeta = Deny("If you see this something went terribly wrong")

    @JvmOverloads
    fun <T> Player.withGameState(playerState: PlayerState? = getMetadata(), block: (PlayerState) -> T) =
        playerState?.let(block)

    fun Player.isAllowedToJoinDressingRoom(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

        if (isInDressingRoom(playerState)) return denyWhileInDressingRoom
        if (isInRide(playerState)) return denyWhileInRide
        if (isInMiniGame(playerState)) return denyWhileInMinigame

        val allowStateManagement = allowStateManagement(playerState, solveProblems = solveProblems)
        if (allowStateManagement is Deny) {
            return allowStateManagement
        }

        if (isInKart(playerState)) {
            if (!solveProblems || playerState.kart?.requestDestroy() != true)
                return denyWhileInKart
        }
        if (isInMiniGameLobby(playerState)) {
            if (!solveProblems || !MinigameManager.leaveLobby(this))
                return denyWhileInMinigameLobby
        }

        return Allow
    }

    fun Player.isAllowedToManuallyJoinRide(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

        if (isInMiniGame(playerState)) return denyWhileInMinigame
        if (isInRide(playerState)) return denyWhileInRide
        if (isInMiniGameLobby(playerState)) {
            if (!solveProblems || !MinigameManager.leaveLobby(this))
                return denyWhileInMinigameLobby
        }
        if (isInKart(playerState)) {
            if (!solveProblems || playerState.kart?.requestDestroy() != true)
                return denyWhileInKart
        }

        return Allow
    }

    fun Player.isAllowedToJoinRideQueue(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

//        if (isInKart(playerState)) return denyWhileInKart
        if (isInRideQueue(playerState)) return denyWhileInRideQueue
        if (isInMiniGame(playerState)) return denyWhileInMinigame
        if (isInRide(playerState)) return denyWhileInRideQueue

        return Allow
    }

    fun Player.isAllowedToJoinMinigameLobby(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

        if (isInRide(playerState)) return denyWhileInRide
        if (isInDressingRoom(playerState)) return denyWhileInDressingRoom

        if (isInKart(playerState)) {
            if (!solveProblems || playerState.kart?.requestDestroy() != true)
                return denyWhileInKart
        }

        return Allow
    }

    fun Player.isAllowedToManuallySpawnKart(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

        if (isInMiniGame(playerState)) return denyWhileInMinigame
        if (isInRide(playerState)) return denyWhileInRide
        if (isInsideVehicle) return denyWhileInAnyVehicle

        if (isInMiniGameLobby(playerState)) {
            if (!solveProblems || !MinigameManager.leaveLobby(this))
                return denyWhileInMinigameLobby
        }

        return Allow
    }

    fun Player.allowTeleporting(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

        if (FinaleCinematic.isRunning()) return denyWhileFinale

        if (isInMiniGame(playerState)) return denyWhileInMinigame
        if (isInDressingRoom(playerState)) return denyWhileInDressingRoom

        if (isInKart(playerState)) {
            if (!solveProblems || playerState.kart?.requestDestroy() != true)
                return denyWhileInKart
        }

        return Allow
    }

//    fun Player.allowVehicleChange(
//        playerState: PlayerState? = getMetadata(),
//        solveProblems: Boolean = true,
//    ): GrantResult {
//        if (playerState == null) return denyNoMeta
//
//        if (isInKart(playerState)) {
//            if (!solveProblems || playerState.kart?.requestDestroy() != true)
//                return denyWhileInKart
//        }
//
//        return Allow
//    }

    // Allow for full control, like dressing rooms, minigames or rides
    fun Player.allowStateManagement(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

        if (isInMiniGame(playerState)) return denyWhileInMinigame
        if (isInDressingRoom(playerState)) return denyWhileInDressingRoom
        if (isInRide(playerState)) return denyWhileInRide

        if (isInMiniGameLobby(playerState)) {
            if (!solveProblems || !MinigameManager.leaveLobby(this))
                return denyWhileInMinigameLobby
        }

        return Allow
    }

    fun Player.allowChangingLocalTime(
        playerState: PlayerState? = getMetadata(),
        solveProblems: Boolean = true,
    ): GrantResult {
        if (playerState == null) return denyNoMeta

        if (isInMiniGame(playerState)) return denyWhileInMinigame
        if (isInDressingRoom(playerState)) return denyWhileInDressingRoom
        if (isInRide(playerState)) return denyWhileInRide

        return Allow
    }

    class PlayerState(
        player: Player
    ) : BasePlayerMetadata(player) {
        var operatingRide: OperableRide? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} operableRide set to ${value?.displayName()}" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already at operableRide ${field?.displayName()} (setting to ${value.displayName()})" }
                }
                field = value
            }

        var ride: RideInstance? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} ride set to ${value?.displayName()}" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already in ride ${field?.displayName()} (setting to ${value.displayName()})" }
                }
                field = value
            }

        var dressingRoom: DressingRoom? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} dressingRoom set to ${value?.id}" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already in dressingRoom ${field?.id} (setting to ${value.id})" }
                }
                field = value
            }

        fun clearRide(ride: RideInstance) {
            if (this.ride !== null && this.ride !== ride) {
                logcat(
                    LogPriority.WARN,
                    logToCrew = true
                ) { "${player().name} tried to clear ride ${ride.displayName()}, but this ride doesn't match the current one ${this.ride?.displayName()}" }
            }
            this.ride = null
        }

        var rideQueue: RideQueue? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} rideQueue set to ${value?.id} for ${value?.ride?.displayName()}" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already in queue ${field?.ride?.displayName()} (setting to ${value.ride.displayName()})" }
                }
                field = value
            }

        var minigame: Minigame? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} minigame set to ${value?.displayName}" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already in minigame ${field?.internalName} (setting to ${value.internalName})" }
                }
                field = value
            }

        var minigameLobby: BaseLobby? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} minigameLobby set to ${value?.id} for ${value?.minigame?.displayName}" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already in lobby ${field?.id} (setting to ${value.id})" }
                }
                field = value
            }

        var kart: Kart? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} kart set to $value" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already in kart" }// ${field?.displayName()} (setting to ${value.displayName()})" }
                }
                field = value
            }

        var jumpPuzzle: JumpPuzzle? = null
            set(value) {
                logcat(LogPriority.DEBUG) { "${player().name} jumpPuzzle set to ${value?.gameId}" }
                if (value !== null && field !== null) {
                    logcat(
                        LogPriority.WARN,
                        logToCrew = true
                    ) { "${player().name} is already in jump puzzle ${field?.gameId} (setting to ${value.gameId})" }
                }
                field = value
            }

        override fun debugComponent() =
            Component.text("jumpPuzzle=${jumpPuzzle?.gameId} kart=${kart != null} lobby=${minigameLobby?.id} minigame=${minigame?.internalName} rideQueue=${rideQueue?.id} dressingRoom=${dressingRoom?.id} ride=${ride?.id} operating=${operatingRide?.id}")

        @GenerateService
        class Generator : PlayerMetaFactory() {
            override fun create(player: Player) = player.getOrCreateMetadata { PlayerState(player) }
        }
    }

//    data class ControllerLock(
//        val allowTeleporting: Boolean,
//        val allowVehicleChanging: Boolean,
//    )
}