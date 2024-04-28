package net.craftventure.database.extension

import net.craftventure.database.generated.cvdata.tables.pojos.PlayerEquippedItem
import net.craftventure.database.type.EquippedItemSlot


fun Collection<PlayerEquippedItem>.firstOfSlot(slot: EquippedItemSlot) = this.firstOrNull { it.slot == slot }