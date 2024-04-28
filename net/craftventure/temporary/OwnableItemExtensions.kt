package net.craftventure.temporary

import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.database.model.ownable.Predecessors
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem


fun OwnableItem.getOwnableItemMetadataUnsafe(): OwnableItemMetadata? {
    val metadata = metadata
    if (metadata.isNullOrBlank()) return null
    return CvMoshi.adapter(OwnableItemMetadata::class.java).fromJson(metadata)
}

fun OwnableItem.getOwnableItemMetadata(): OwnableItemMetadata? {
    return try {
        getOwnableItemMetadataUnsafe()
    } catch (e: Exception) {
        logcat(
            logToCrew = true,
            priority = LogPriority.ERROR
        ) { "Failed to parse item meta of item ${id}: ${e.message}" }
        e.printStackTrace()
        null
    }
}

fun OwnableItem.getPredecessorsMetaUnsafe(): Predecessors? {
    val predecessors = predecessors
    if (predecessors.isNullOrBlank()) return null
    return CvMoshi.adapter(Predecessors::class.java).fromJson(predecessors)
}

fun OwnableItem.getPredecessorsMeta(): Predecessors? {
    return try {
        getPredecessorsMetaUnsafe()
    } catch (e: Exception) {
        logcat(
            logToCrew = true,
            priority = LogPriority.ERROR
        ) { "Failed to parse predecessor meta of item ${id}: ${e.message}" }
        e.printStackTrace()
        null
    }
}