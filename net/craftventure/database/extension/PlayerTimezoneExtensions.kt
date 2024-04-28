package net.craftventure.database.extension

import net.craftventure.database.generated.cvdata.tables.pojos.PlayerTimezone
import java.time.ZoneId

fun PlayerTimezone.toZoneId(fallback: ZoneId? = ZoneId.systemDefault()) = try {
    zone?.let { ZoneId.of(it) } ?: fallback
} catch (e: Exception) {
    fallback
}