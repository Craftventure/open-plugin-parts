package net.craftventure.temporary

import net.craftventure.core.async.executeSync
import net.craftventure.core.map.renderer.MapManager
import net.craftventure.core.map.renderer.TeamScoreScoreboardRenderer
import net.craftventure.database.generated.cvdata.tables.pojos.TeamScore
import net.craftventure.database.repository.BaseIdRepository

class TeamScoreListener : BaseIdRepository.Listener<TeamScore>() {
    override fun onMerge(item: TeamScore) {
        executeSync {
            invalidate(item)
        }
    }

    override fun onInsert(item: TeamScore) {
        executeSync {
            invalidate(item)
        }
    }

    private fun invalidate(teamScore: TeamScore) {
        MapManager.instance.invalidate<TeamScoreScoreboardRenderer>(teamScore.target!!)
    }
}