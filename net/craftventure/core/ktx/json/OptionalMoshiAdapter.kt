package net.craftventure.core.ktx.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

class OptionalJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        return if (type is ParameterizedType && type.rawType == Optional::class.java) {
            OptionalAdapter(
                moshi,
                type.actualTypeArguments[0]
            )
        } else null
    }

    private class OptionalAdapter(private val moshi: Moshi, private val type: Type) :
        JsonAdapter<Optional<*>>() {
        private var adapter: JsonAdapter<Any>? = null

        @Throws(IOException::class)
        override fun toJson(out: JsonWriter, value: Optional<*>?) {
            val serializeNulls = out.serializeNulls
            try {
                if (value != null) {
                    if (adapter == null) {
                        adapter = moshi.adapter(type)
                    }
//                    Logger.debug("Adapter for Optional<$type> = $adapter")
                    val realValue = value.orElseGet(null)
                    if (realValue == null)
                        out.serializeNulls = true
                    adapter!!.toJson(out, realValue)
                } else {
                    out.nullValue()
                }
            } finally {
                out.serializeNulls = serializeNulls
            }
        }

        @Throws(IOException::class)
        override fun fromJson(reader: JsonReader): Optional<*>? {
            if (!reader.hasNext()) {
                reader.nextNull<Optional<*>>()
                return null
            }
            if (reader.peek() == JsonReader.Token.NULL) {
                reader.nextNull<Optional<*>>()
                return Optional.ofNullable(null)
            }
            if (adapter == null) {
                adapter = moshi.adapter(type)
            }
            return Optional.ofNullable(adapter!!.fromJson(reader))
        }
    }
}