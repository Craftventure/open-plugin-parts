package net.craftventure.core.ride.shooter.hitaction

import com.squareup.moshi.JsonClass
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.shooter.ShooterScene
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class DeltaBankAccountBalanceAction(
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
        MainRepositoryProvider.bankAccountRepository.delta(
            player.uniqueId,
            data.accountType,
            data.value,
            TransactionType.SHOOTER_RIDE_REWARD
        )
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
        val value: Long,
        val accountType: BankAccountType = BankAccountType.VC,
    ) : EntityHitAction.Data() {
        override fun toAction(): EntityHitAction = DeltaBankAccountBalanceAction(this)
    }

    companion object {
        const val type = "delta_bank_account"
    }
}