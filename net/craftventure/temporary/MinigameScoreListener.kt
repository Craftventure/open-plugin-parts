package net.craftventure.temporary

import net.craftventure.core.map.renderer.MapManager
import net.craftventure.database.generated.cvdata.tables.pojos.MinigameScore
import net.craftventure.database.repository.BaseIdRepository

class MinigameScoreListener : BaseIdRepository.Listener<MinigameScore>() {
    override fun onMerge(item: MinigameScore) {
        MapManager.instance.invalidateGame(item.game)
    }

    override fun onInsert(item: MinigameScore) {
        MapManager.instance.invalidateGame(item.game)
    }

    override fun onUpdate(item: MinigameScore) {
        MapManager.instance.invalidateGame(item.game)
    }

    override fun onDelete(item: MinigameScore) {
        MapManager.instance.invalidateGame(item.game)
    }
}