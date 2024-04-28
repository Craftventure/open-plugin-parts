package net.craftventure.database.bukkit.extensions

import java.util.*

@Deprecated(message = "Not setup yet")
fun String.hasEverJoinedCv() = false//RepositoryDelegate.guestStatRepository.getOnly(this) != null

@Deprecated(message = "Not setup yet")
fun String.asNameToUuid(): UUID? {
//    val guestStat = RepositoryDelegate.guestStatRepository.getOnly(this)
//    if (guestStat != null) {
//        return UUID.fromString(guestStat.uuid)
//    }
    return null
}

@Deprecated(message = "Not setup yet")
fun String.asUuidToName(): String {
//    val uuid = PlayerUtil.get(this, false)
//    if (uuid != null) {
//        val guestStat =RepositoryDelegate.guestStatRepository.getOnly(uuid)
//        if (guestStat != null) {
//            return guestStat.lastKnownName
//        }
//    }
    return this
}

@Deprecated(message = "Not setup yet")
fun UUID.toName(): String {
//    val guestStat = CraftventureCore.getGuestStatDatabase().getOnly(this)
//    if (guestStat != null) {
//        return guestStat.lastKnownName
//    }
    return this.toString()
}