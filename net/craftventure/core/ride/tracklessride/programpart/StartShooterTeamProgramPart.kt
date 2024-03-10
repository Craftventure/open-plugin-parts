package net.craftventure.core.ride.tracklessride.programpart

import com.squareup.moshi.JsonClass
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class StartShooterTeamProgramPart(
    private val data: Data,
    scene: TracklessRideScene,
) : ProgramPart<Any>(scene) {
    override val type: String = StartShooterTeamProgramPart.type
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
        val team = shooterRideContext.createTeam(car.playerPassengers.toSet())
        car.team = team
        if (shooterRideContext.config.gunItemAlwaysInHand) {
            team.players.forEach { player ->
                player.player.inventory.heldItemSlot = EquipmentManager.SLOT_WEAPON
                EquipmentManager.reapply(player.player)
            }
        }
        return ExecuteResult.DONE
    }

    @JsonClass(generateAdapter = true)
    class Data : ProgramPartData<Any>() {
        override fun toPart(scene: TracklessRideScene): ProgramPart<Any> = StartShooterTeamProgramPart(this, scene)
    }

    companion object {
        const val type = "start_shooter_team"
    }
}