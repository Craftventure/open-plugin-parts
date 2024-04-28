package net.craftventure.temporary

import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.database.wrap.WrappedPlayerOwnedItem

fun WrappedPlayerOwnedItem.getOwnableItemMetadata() = source.getOwnableItemMetadata()

fun PlayerOwnedItem.getOwnableItemMetadata(): OwnableItemMetadata? {
//    if (this.cachedMetadata != null) return cachedMetadata
    if (this.metadata == null) return null
    return /*cachedMetadata =*/ CvMoshi.adapter(OwnableItemMetadata::class.java).fromJson(this.metadata)
//    return cachedMetadata
}