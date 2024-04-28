package net.craftventure.core.extension

import co.aikar.timings.Timings
import net.craftventure.core.CraftventureCore
import net.craftventure.core.utils.PlayerUtil
import net.craftventure.database.MainRepositoryProvider
import java.util.*


//fun String.broadcastAsDebugTimings() {
//    if (CraftventureCore.isOnMainThread()) {
//        executeAsync {
//            this.broadcastAsDebugTimings()
//        }
//        return
//    }
//    Logger.info(this, logToCrew = false)
//    Bukkit.getOnlinePlayers()
//            .forEach {
//                if (it.isCrew() && MainRepositoryProvider.playerKeyValueRepository.getValue(it.uniqueId,
//                                PlayerKeyValueDatabase.KEY_TIMINGS_DEBUG) != null)
//                    it.sendMessage(this)
//            }
//}

fun String.hasEverJoinedCv() = MainRepositoryProvider.guestStatRepository.getByNameOrUuid(this) != null

fun String.asNameToUuid(): UUID? {
    val guestStat = MainRepositoryProvider.guestStatRepository.getByNameOrUuid(this)
    if (guestStat != null) {
        return guestStat.uuid
    }
    return null
}

fun String.asUuidToName(): String {
    val uuid = PlayerUtil.get(this, false)
    if (uuid != null) {
        val guestStat = MainRepositoryProvider.guestStatRepository.findSilent(uuid)
        if (guestStat != null) {
            return guestStat.lastKnownName!!
        }
    }
    return this
}