package net.craftventure.database.bukkit.listener

import net.craftventure.bukkit.ktx.util.ItemStackUtils2
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.generated.cvdata.tables.pojos.Shop
import net.craftventure.database.generated.cvdata.tables.pojos.ShopOffer
import net.craftventure.database.repository.BaseIdRepository
import java.util.*

object ShopCacheListener : BaseIdRepository.Listener<Shop>() {
    private val cachedShopHashMap = hashMapOf<String, CachedShop>()

    override fun invalidateCaches() {
        cachedShopHashMap.clear()
    }

    override fun onInsert(item: Shop) {
        handle(item)
    }

    override fun onMerge(item: Shop) {
        handle(item)
    }

    override fun onUpdate(item: Shop) {
        handle(item)
    }

    override fun onDelete(item: Shop) {
        cachedShopHashMap.remove(item.id)
    }

    override fun onRefresh(item: Shop) {
        handle(item)
    }

    private fun handle(item: Shop) {
        cached(item.id!!)?.let {
            cachedShopHashMap[item.id!!] = it
        }
    }

    fun cached(id: String): CachedShop? {
        val id = id.lowercase(Locale.getDefault())
        if (cachedShopHashMap.containsKey(id))
            return cachedShopHashMap[id]
        val shop = MainRepositoryProvider.shopRepository.findSilent(id)
        if (shop != null && shop.enabled!! || id.equals("all", ignoreCase = true)) {
            val cachedShop = CachedShop(shop)
            cachedShopHashMap[id] = cachedShop
            return cachedShop
        }
        return null
    }

    class CachedShop internal constructor(shop: Shop?) {
        val shop: Shop
        val cachedOffers = ArrayList<CachedOffer>()

        init {
            var shop = shop
            if (shop == null) {
                shop = Shop("all", "All", true)
            }
            this.shop = shop

            val shopOffers = MainRepositoryProvider.shopOfferRepository.getOffersByShopId(shop.id!!)
            val ownableItemDatabase = MainRepositoryProvider.ownableItemRepository
            val itemStackDataDatabase = MainRepositoryProvider.itemStackDataRepository
            for (shopOffer in shopOffers) {
                val ownableItem = ownableItemDatabase.findSilent(shopOffer.shopItemId!!)
                if (ownableItem != null) {
                    if (ownableItem.enabled!!) {
                        var shopItemStackData = itemStackDataDatabase.findSilent(ownableItem.guiItemStackDataId!!)
                        if (shopItemStackData != null) {
                            val cachedOffer = CachedOffer(shopOffer, ownableItem, shopItemStackData)
                            cachedOffers.add(cachedOffer)
                        } else {
                            Logger.warn("Failed to find ItemStackData " + ownableItem.guiItemStackDataId + " of OwnableItem " + ownableItem.id)
                        }
                    }
                } else {
                    Logger.warn("Failed to find OwnableItem " + shopOffer.shopItemId + " of offer for shop " + shopOffer.shopId)
                }
            }
            cachedOffers.sortBy { it.ownableItem.price }
        }
    }

    data class CachedOffer(
        val shopOffer: ShopOffer,
        val ownableItem: OwnableItem,
        val shopItemStackData: ItemStackData
    ) : Comparable<CachedOffer> {

        override fun compareTo(o: CachedOffer): Int {
            if (shopItemStackData.itemStack != null && o.shopItemStackData.itemStack != null) {
                try {
                    return ItemStackUtils2.getDisplayName(
                        shopItemStackData.itemStack
                    )
                        .compareTo(ItemStackUtils2.getDisplayName(o.shopItemStackData.itemStack))
                } catch (e: Exception) {
                    //                    e.printStackTrace();
                }

            }
            return 0
        }
    }
}