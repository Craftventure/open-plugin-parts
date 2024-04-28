package net.craftventure.core.script

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.script.action.ScriptActionFrame
import java.util.*

@JsonClass(generateAdapter = true)
data class ScriptFrameMap(
    @Expose val updateTick: Long = 0,
    @Expose val repeat: Boolean = true,
    @Expose val playWithoutPlayersInArea: Boolean = false,
    @Expose val targetDuration: Long = 0,
    @Expose val frames: List<ScriptActionFrame<*>> = LinkedList(),
)