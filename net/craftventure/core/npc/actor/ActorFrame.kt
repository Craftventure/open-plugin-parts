package net.craftventure.core.npc.actor

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.actor.action.ActorAction

@JsonClass(generateAdapter = true)
class ActorFrame<T : ActorAction?> @JvmOverloads constructor(
    @field:Expose
    val time: Long,
    @field:Expose
    @field:SerializedName("data")
    val action: T,
    @field:Expose
    val type: Int = 0
)