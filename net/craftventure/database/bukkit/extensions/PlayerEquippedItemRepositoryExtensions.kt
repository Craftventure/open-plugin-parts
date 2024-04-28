package net.craftventure.database.bukkit.extensions

import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.repository.PlayerEquippedItemRepository
import net.craftventure.database.type.EquippedItemSlot
import org.bukkit.entity.Player
import java.util.*


fun PlayerEquippedItemRepository.update(
    player: Player,
    slot: EquippedItemSlot,
    item: OwnableItem?,
    source: UUID?
): Boolean =
    update(player.uniqueId, slot, item, source)

fun PlayerEquippedItemRepository.update(
    playerId: UUID,
    slot: EquippedItemSlot,
    item: OwnableItem?,
    source: UUID?
): Boolean =
    update(playerId, slot, item?.id, source)