package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.TeamScore
import net.craftventure.database.generated.cvdata.tables.pojos.TeamScoreMember
import java.time.LocalDateTime
import java.util.*

class SaveTeamScoreProgramPart(
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
        val teams = group.cars.mapNotNull { it.team }
        if (teams.isEmpty()) return ExecuteResult.DONE

        val totalScore = teams.sumOf { it.score }
        if (totalScore == 0) return ExecuteResult.DONE

        val members = teams.flatMap { it.players }

        val teamScore =
            TeamScore(team = UUID.randomUUID(), target = "mat", score = totalScore, at = LocalDateTime.now())
        val teamScoreMembers = members.map {
            TeamScoreMember(
                id = UUID.randomUUID(),
                team = teamScore.team!!,
                member = it.player.uniqueId,
                score = it.score,
                crew = it.player.isCrew(),
            )
        }

        executeAsync {
            try {
//                logcat { "Saving team with ${members.size} members" }
                if (MainRepositoryProvider.teamScoreRepository.create(teamScore)) {
//                    logcat { "Saving ${teamScoreMembers.size} membrs" }
                    MainRepositoryProvider.teamScoreMemberRepository.createIfNotExists(teamScoreMembers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    class Data : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = SaveTeamScoreProgramPart(this, scene)
    }

    companion object {
        const val type = "save_team_score"
    }
}