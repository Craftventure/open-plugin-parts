package net.craftventure.core.ktx.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import net.craftventure.core.ktx.extension.asTicks
import net.craftventure.core.ktx.extension.asTicksInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class KotlinDurationAdapter : JsonAdapter<DurationJson>() {
    override fun fromJson(reader: JsonReader): DurationJson? {
        return when (reader.peekJson().peek()) {
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                DurationJson(Duration.parse(value))
            }

            JsonReader.Token.NUMBER -> {
                val value = reader.nextDouble()
                DurationJson(value.toDuration(DurationUnit.SECONDS))
            }

            else -> {
                reader.skipValue()
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: DurationJson?) {
        writer.value(value?.toString())
    }
}

/**
 * Temporary workaround because value classes don't work with Moshi yet
 */
class DurationJson(
    val duration: Duration,
) {
    override fun equals(other: Any?): Boolean = duration.equals(other)
    override fun hashCode(): Int = duration.hashCode()
    override fun toString(): String = duration.toString()

    fun asTicks() = duration.asTicks()
    fun asTicksInt() = duration.asTicksInt()
    val inWholeSeconds by lazy { duration.inWholeSeconds }
    val inWholeMilliseconds by lazy { duration.inWholeMilliseconds }

    fun toDouble(unit: DurationUnit) = duration.toDouble(unit)
}