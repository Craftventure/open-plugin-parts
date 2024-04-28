package net.craftventure.temporary

import net.craftventure.core.map.renderer.MapManager
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerKeyValue
import net.craftventure.database.repository.BaseIdRepository

class PlayerKeyValueMapListener : BaseIdRepository.Listener<PlayerKeyValue>() {
    override fun onMerge(item: PlayerKeyValue) {
        handle(item)
    }

    override fun onInsert(item: PlayerKeyValue) {
        handle(item)
    }

    override fun onUpdate(item: PlayerKeyValue) {
        handle(item)
    }

    override fun onDelete(item: PlayerKeyValue) {
        handle(item)
    }

    override fun onRefresh(item: PlayerKeyValue) {
        handle(item)
    }

    private fun handle(item: PlayerKeyValue) {
        MapManager.instance.invalidatePlayerKeyValueKey(item.key)
    }
}