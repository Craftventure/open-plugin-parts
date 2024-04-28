package net.craftventure.core.ride.shooter.hitaction

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.shooter.ShooterScene
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class DeltaScoreHitAction(
    private val data: Data,
) : EntityHitAction() {
    override val type: String = DeltaScoreHitAction.type

    override fun execute(
        ride: OperableRide,
        context: ShooterRideContext,
        team: ShooterRideContext.Team,
        player: Player,
        scene: ShooterScene,
        entity: ShooterScene.ManagedEntity,
    ) {
        team.dataFor(player)?.apply {
            this.hit(data.value)
        }
        player.sendTitleWithTicks(
            5,
            20,
            5,
            NamedTextColor.GOLD,
            null,
            if (data.value >= 0) NamedTextColor.GREEN else NamedTextColor.RED,
            "${if (data.value >= 0) "+" else "-"}${data.value}"
        )
    }

    @JsonClass(generateAdapter = true)
    data class Data(
        val value: Int,
    ) : EntityHitAction.Data() {
        override fun toAction(): EntityHitAction = DeltaScoreHitAction(this)
    }

    companion object {
        const val type = "delta_score"
    }
}