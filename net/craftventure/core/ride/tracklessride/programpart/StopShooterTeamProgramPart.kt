package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.type.MinigameScoreType
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.util.*

class StopShooterTeamProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = Companion.type
    override fun createInitialState(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
    ): Any = Unit

    override fun execute(
        ride: TracklessRide,
        group: TracklessRideCarGroup,
        car: TracklessRideCar,
        state: Any
    ): ExecuteResult {
        val shooterRideContext = ride.shooterRideContext ?: return ExecuteResult.DONE
        val team = car.team
        if (team != null) {
            val score = team.score
            val scoresToSave = team.players.map {
                MinigameScore(
                    UUID.randomUUID(),
                    it.player.uniqueId,
                    ride.id,
                    score,
                    LocalDateTime.now(),
                    MinigameScoreType.TOTAL,
                    null,
                    it.player.isCrew()
                )
            }
            executeAsync {
                scoresToSave.forEach { scoreToSave ->
                    val success = MainRepositoryProvider.minigameScoreRepository.createSilent(scoreToSave)
                    if (success) {
                        val player = scoreToSave.uuid?.let { Bukkit.getPlayer(it) }
                        player?.sendMessage(CVTextColor.serverNotice + "Your score of ${scoreToSave.score} at ${ride.ride?.displayName} has been saved")
                    }
                }
            }
            shooterRideContext.removeTeam(team)
            car.team = null

            if (shooterRideContext.config.gunItemAlwaysInHand)
                team.players.forEach { player ->
                    EquipmentManager.reapply(player.player)
                }
        }
        return ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    class Data : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = StopShooterTeamProgramPart(this, scene)
    }

    companion object {
        const val type = "stop_shooter_team"
    }
}