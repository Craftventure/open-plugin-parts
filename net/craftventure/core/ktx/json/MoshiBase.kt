package net.craftventure.core.ktx.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.File

object MoshiBase {
    private var builder = Moshi.Builder()
    private var dirty = false

    @JvmStatic
    var moshi: Moshi
        get() {
            if (dirty) {
                field = builder.build()
                dirty = false
            }
            return field
        }
        private set

    init {
        builder.add(SerializeNulls.JSON_ADAPTER_FACTORY)

        moshi = builder.build()
    }

    private fun markDirty() {
        dirty = true
    }

    fun withBuilder(): Moshi.Builder {
        markDirty()
        return builder
    }

    fun <T> JsonAdapter<T>.parseFile(file: File) = fromJson(file.readText())
}

val CvMoshi get() = MoshiBase.moshi

inline fun <reified T> parseJson(json: String) = MoshiBase.moshi.adapter(T::class.java).fromJson(json)
inline fun <reified T> T.toJson(moshi: Moshi = MoshiBase.moshi) = moshi.adapter(T::class.java).toJson(this)
inline fun <reified T> T.toJsonIndented(moshi: Moshi = MoshiBase.moshi, indent: String = "  ") =
    moshi.adapter(T::class.java).indent(indent).toJson(this)