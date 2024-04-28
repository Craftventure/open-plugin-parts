package net.craftventure.database.bukkit.extensions

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.repository.ItemStackDataRepository
import org.bukkit.inventory.ItemStack

fun ItemStackDataRepository.getItemStackById(id: String): ItemStack? {
    return findCached(id)?.itemStack
}

fun ItemStackDataRepository.updateAllItemstacks() {
    val items = itemsPojo()
    items.map { itemStackData ->
        Logger.debug("Migrating ${itemStackData.id}")
        val itemStack = itemStackData.itemStack!!

        // TODO: Reimplement this

//                if (itemStack.durability > 0) {
//                    Logger.debug("Migrating durability of ${itemStackData.id} to customModelData")
//                    itemStack.updateMeta<ItemMeta> { setCustomModelData(itemStack.durability.toInt()) }
//                    itemStack.durability = 0
//                }
//
//                if (itemStack.type == Material.FIREWORK_STAR) {
//                    Logger.debug("Migrating firework star of ${itemStackData.id} to customModelData")
//                    itemStack.updateMeta<ItemMeta> { setCustomModelData(1) }
//                }
//
//                if (itemStack.type == Material.WOODEN_SHOVEL || itemStack.type == Material.WOODEN_PICKAXE || itemStack.type == Material.WOODEN_SWORD) {
//                    Logger.debug("Migrating wood item of ${itemStackData.id} to customModelData")
//                    itemStack.updateMeta<ItemMeta> { setCustomModelData(1) }
//                }
//
//                if (itemStack.type == Material.PLAYER_HEAD) {
//                    itemStack.updateMeta<SkullMeta> {
//                        val currentId = this.playerProfile?.id
//                        if (currentId != null) {
//                            val fixedUuid = currentId.withV2Marker()
//                            if (fixedUuid != currentId) {
//                                Logger.debug("Migrating head ${itemStackData.id} to V2 uuid")
//                                this.playerProfile = this.playerProfile?.apply {
//                                    this.id = fixedUuid
//                                }
//                            }
//                        }
//                    }
//                }

        itemStackData.itemstack = itemStack.serializeAsBytes()
    }
    update(items)
}