package net.craftventure.core.extension

import net.craftventure.database.MainRepositoryProvider
import java.util.*

fun UUID.toName(): String {
    val guestStat = MainRepositoryProvider.guestStatRepository.findSilent(this)
    if (guestStat != null) {
        return guestStat.lastKnownName!!
    }
    return this.toString()
}