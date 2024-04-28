package net.craftventure.database.bukkit.extensions

import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.database.type.BankAccountType
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

val BankAccountType.itemRepresentation: ItemStack
    get() = ItemStack(Material.GOLD_NUGGET)
        .updateMeta<ItemMeta> { setCustomModelData(modelDataId) }