package net.craftventure.core.ride.shooter.hitaction

import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.shooter.ShooterScene
import org.bukkit.entity.Player

abstract class EntityHitAction {
    abstract val type: String

    abstract fun execute(
        ride: OperableRide,
        context: ShooterRideContext,
        team: ShooterRideContext.Team,
        player: Player,
        scene: ShooterScene,
        entity: ShooterScene.ManagedEntity,
    )

    abstract class Data {
        abstract fun toAction(): EntityHitAction
    }
}