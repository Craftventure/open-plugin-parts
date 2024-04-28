package net.craftventure.temporary

import net.craftventure.core.utils.ItemStackUtils
import net.craftventure.database.generated.cvdata.tables.pojos.SlotMachineItem
import org.bukkit.inventory.ItemStack

fun SlotMachineItem.getResolvedItem(): ItemStack? = ItemStackUtils.fromString(itemId)