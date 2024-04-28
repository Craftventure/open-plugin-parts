package net.craftventure.core.ktx.extension

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

val Date.asLocalDateTime: LocalDateTime
    get() = this.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

val LocalDateTime.utcMillis: Long
    get() = toInstant(ZoneOffset.UTC).toEpochMilli()