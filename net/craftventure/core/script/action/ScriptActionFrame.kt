package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ScriptActionFrame<T : ScriptAction?>(
    @Expose val time: Long,
    @Expose val action: T
)