package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.ShopOfferDao
import net.craftventure.database.generated.cvdata.tables.pojos.ShopOffer
import net.craftventure.database.generated.cvdata.tables.records.ShopOfferRecord
import org.jooq.Configuration
import java.util.*

class ShopOfferRepository(
    configuration: Configuration
) : BaseIdRepository<ShopOfferRecord, ShopOffer, UUID>(
    ShopOfferDao(configuration),
    shouldCache = true
) {
    fun getOffersByShopId(id: String): List<ShopOffer> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.SHOP_OFFER.SHOP_ID.eq(id))
            .fetchInto(dao.type)
    }
}