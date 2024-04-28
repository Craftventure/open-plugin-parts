package net.craftventure.database.bukkit.extensions

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.repository.PlayerOwnedItemRepository

fun PlayerOwnedItemRepository.getItemName(id: String): String =
    (MainRepositoryProvider.ownableItemRepository.findCached(id)
        ?.let {
            Logger.debug(
                "ItemName ${it.guiItemStackDataId}/${
                    MainRepositoryProvider.itemStackDataRepository.findCached(
                        it.guiItemStackDataId!!
                    ) != null
                }"
            )
            it.type!!.displayName + " " + (MainRepositoryProvider.itemStackDataRepository.findCached(it.guiItemStackDataId!!)
                ?.let {
                    it.overridenTitle// ?: it.itemStack?.displayName()
                } ?: "")
        } ?: id).trim()