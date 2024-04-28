package net.craftventure.database.bukkit.extensions

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.repository.GuestStatRepository
import org.bukkit.entity.Player
import java.time.LocalDateTime

fun GuestStatRepository.update(player: Player) = withDslIgnoreErrors(false) { dsl ->
    dsl.update(table)
        .set(Cvdata.CVDATA.GUEST_STAT.LAST_KNOWN_NAME, player.name)
        .set(Cvdata.CVDATA.GUEST_STAT.LAST_SEEN, LocalDateTime.now())
        .where(Cvdata.CVDATA.GUEST_STAT.UUID.eq(player.uniqueId))
        .execute() == 1
}