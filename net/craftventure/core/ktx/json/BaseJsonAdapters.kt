package net.craftventure.core.ktx.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class BaseJsonAdapters() {
    // LocalDateTime as ISO_LOCAL_DATE_TIME
    @FromJson
    internal fun isoLocalDateTimeFromJson(json: String?): LocalDateTime? = json?.let {
        LocalDateTime.parse(json)
    }

    @ToJson
    internal fun isoLocaleDateTimeToJson(dateTime: LocalDateTime?): String? = dateTime?.let {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime)
    }

    // OffsetDateTime as ISO_OFFSET_DATE_TIME
    @FromJson
    internal fun offsetDateTimeJson(json: String?): OffsetDateTime? = json?.let {
        OffsetDateTime.parse(json)
    }

    @ToJson
    internal fun offsetDateTimeToJson(dateTime: OffsetDateTime?): String? = dateTime?.let {
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime.truncatedTo(ChronoUnit.SECONDS))
    }

    @FromJson
    internal fun uuidFromJson(json: String?): UUID? = json?.let { UUID.fromString(json) }

    @ToJson
    internal fun uuidToJson(uuid: UUID?): String? = uuid?.toString()
}