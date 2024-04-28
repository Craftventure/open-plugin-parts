package net.craftventure.database.bukkit.listener

import net.craftventure.database.bukkit.extensions.createItemStack
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.repository.BaseIdRepository
import org.bukkit.inventory.ItemStack

object ItemStackDataCacheListener : BaseIdRepository.Listener<ItemStackData>() {
    val items = hashMapOf<String, ItemStack>()

    override fun invalidateCaches() {
        items.clear()
    }

    override fun onInsert(item: ItemStackData) {
        handle(item)
    }

    override fun onUpdate(item: ItemStackData) {
        handle(item)
    }

    override fun onMerge(item: ItemStackData) {
        handle(item)
    }

    override fun onDelete(item: ItemStackData) {
        items.remove(item.id)
    }

    override fun onRefresh(item: ItemStackData) {
        handle(item)
    }

    private fun handle(item: ItemStackData) {
        item.createItemStack()?.let {
            items[item.id!!] = it
        }
    }
}