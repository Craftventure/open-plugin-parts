package net.craftventure.core.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import java.io.IOException
import java.lang.reflect.Type

class DoubleStringAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
//        Logger.debug("type=$type")
        return if (type is Class<*> && (type == Double::class.java || type == Double::class.javaObjectType)) {
//            Logger.debug("Using adapter")
            DoubleAdapter(moshi, type, this)
        } else null
    }

    private class DoubleAdapter(
        private val moshi: Moshi,
        private val type: Type,
        private val factory: DoubleStringAdapter
    ) : JsonAdapter<Double>() {

        @Throws(IOException::class)
        override fun toJson(out: JsonWriter, value: Double?) {
            out.value(value)
        }

        @Throws(IOException::class)
        override fun fromJson(reader: JsonReader): Double? {
            //            Logger.debug("Peeked ${peekValue}")
            @Suppress("NON_EXHAUSTIVE_WHEN")
            return when (reader.peekJson().peek()) {
                JsonReader.Token.STRING -> {
                    //                    Logger.debug("String")
                    val value = reader.nextString()
                    if (value == "random") return CraftventureCore.getRandom().nextDouble()
                    //                    Logger.debug("value=$value")
                    when {
                        value.contains("%") -> value.split("%")[0].toDoubleOrNull()?.let { it / 100.0 }
                        value.endsWith("/kmh2bpt") ->
                            CoasterMathUtils.kmhToBpt(value.removeSuffix("/kmh2bpt").toDouble())

                        else -> value?.toDouble()
                    }
                }

                else -> moshi.nextAdapter<Double>(factory, Double::class.java, emptySet()).fromJson(reader)
            }
        }
    }
}