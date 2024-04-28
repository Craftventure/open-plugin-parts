package net.craftventure.core.npc.actor.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.NpcEntity
import org.bukkit.Location
import org.bukkit.World

@JsonClass(generateAdapter = true)
data class ActionAnimatedMove(
    @field:Expose val x: Double,
    @field:Expose val y: Double,
    @field:Expose val z: Double,
    @field:Expose val yaw: Float,
    @field:Expose val headYaw: Float,
    @field:Expose val pitch: Float,
    @field:Expose val isTeleport: Boolean,
) : ActorAction() {
    fun getLocation(world: World?): Location {
        return Location(world, x, y, z, yaw, pitch)
    }

    override fun executeAction(npcEntity: NpcEntity?) {
        npcEntity!!.move(x, y, z, yaw, pitch)
        npcEntity.moveHead(headYaw)
    }

    override val actionTypeId: Int
        get() = Type.ANIMATED_MOVE
}