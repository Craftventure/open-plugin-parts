package net.craftventure.core.ride.shooter.hitaction

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.shooter.ShooterScene
import org.bukkit.entity.Player

class DespawnHitAction(
    val data: Data,
) : EntityHitAction() {
    override val type: String = DespawnHitAction.type

    override fun execute(
        ride: OperableRide,
        context: ShooterRideContext,
        team: ShooterRideContext.Team,
        player: Player,
        scene: ShooterScene,
        entity: ShooterScene.ManagedEntity,
    ) {
        entity.despawn(scene.tracker)
    }

    @JsonClass(generateAdapter = true)
    class Data : EntityHitAction.Data() {
        override fun toAction(): EntityHitAction = DespawnHitAction(this)
    }

    companion object {
        const val type = "despawn"
    }
}