package net.craftventure.core.database

import net.craftventure.bukkit.ktx.extension.setColor
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.core.CraftventureCore
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.temporary.getOwnableItemMetadata
import org.bukkit.Color
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkEffectMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta

object ItemStackLoader {
    fun getMeta(ownableItem: OwnableItem?, playerOwnedItem: PlayerOwnedItem? = null): OwnableItemMetadata? =
        ownableItem?.getOwnableItemMetadata()?.mergedWith(playerOwnedItem?.getOwnableItemMetadata())
            ?: playerOwnedItem?.getOwnableItemMetadata()

    fun update(itemStack: ItemStack, ownableItem: OwnableItem?, playerOwnedItem: PlayerOwnedItem? = null): ItemStack {
        val ownableItemMeta = getMeta(ownableItem, playerOwnedItem)
        if (ownableItemMeta != null)
            apply(itemStack, ownableItemMeta)
        return itemStack
    }

    fun apply(itemStack: ItemStack, ownableItemMeta: OwnableItemMetadata): ItemStack {
        itemStack.updateMeta<ItemMeta> {
            if (ownableItemMeta.isRandomColor) {
                val color = Color.fromRGB(CraftventureCore.getRandom().nextInt() and 0x00FFFFFF)
                if (this is PotionMeta) {
                    this.color = color
                } else if (this is FireworkEffectMeta) {
                    this.setColor(color)
                }
            } else
                ownableItemMeta.parsedMainColor?.let { color ->
                    if (this is PotionMeta) {
                        this.color = color
                    } else if (this is FireworkEffectMeta) {
                        this.setColor(color)
                    }
                }
        }
        return itemStack
    }
}